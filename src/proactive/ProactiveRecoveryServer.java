package proactive;

import java.io.IOException;
import java.math.BigInteger;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import main.ConnectionInfo;
import proactive.messages.DumpStateMessage;
import proactive.messages.DumpStateResponseMessage;
import proactive.messages.InternalPRSServerMessage;
import proactive.messages.PRSMessage;
import proactive.messages.PRSMessageType;
import proactive.messages.PRSServerMessage;
import proactive.messages.SendSecretMessage;
import proactive.messages.SendStateMessage;
import proactive.messages.SendVerifierMessage;
import server.SecretShare;
import server.Server;
import utils.SerializationUtil;
import utils.TimeUtility;

public class ProactiveRecoveryServer implements Runnable {

	private DatagramSocket prsSocket;

	private DatagramSocket payloadSocket;

	private int prsId;

	private int l;

	private int t;

	private long timer = 0;

	private static int basePort = 5000;

	private PRSServerKeyManager skm;

	private PRSServerThresholdKeyManager stkm;

	private ConnectionInfo payloadConnectionInfo;

	private HashMap<String, SecretShare> shareDB;

	private HashMap<String, SecretShare> correctState = new HashMap<String, SecretShare>();

	private Map<Integer, ConnectionInfo> serverConnectionInfo;

	private static int wovDuration = 10*60*1000; // Duration of window of
											// vulnerability in ms

	private volatile LinkedBlockingQueue<PRSServerMessage> serverMessages = new LinkedBlockingQueue<PRSServerMessage>(
			10);

	public ProactiveRecoveryServer(int prsId, int serverPort, int servers,
			int threshold) {
		this.l = servers;
		this.t = threshold - 1;
		this.prsId = prsId;
		this.skm = new PRSServerKeyManager(prsId, servers);
		this.stkm = new PRSServerThresholdKeyManager(prsId);
		// try {
		// initKey();
		// } catch (InvalidKeyException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// } catch (NoSuchAlgorithmException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }

		payloadConnectionInfo = new ConnectionInfo("localhost", serverPort
				+ prsId);

		serverConnectionInfo = new HashMap<Integer, ConnectionInfo>();

		// Add connections for all servers except one that matches the id
		// connection)
		for (int i = 0; i < servers; i++) {
			// if (i != serverId)
			serverConnectionInfo.put(i, new ConnectionInfo("localhost",
					basePort + i));
		}

		try {
			this.prsSocket = new DatagramSocket(basePort + prsId);
			this.payloadSocket = new DatagramSocket(4000 + prsId);
			// println(this.prsSocket.getSendBufferSize());
			// println(this.prsSocket.getReceiveBufferSize());
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	// public void initKey() throws NoSuchAlgorithmException,
	// InvalidKeyException {
	// if (privateKey == null) {
	//
	// this.privateKey = KeyUtility.getPrivateKey(Constants.CONFIG_DIR
	// + "/" + Constants.PRS_PRIVATE_KEY_FILE + this.prsId);
	//
	// }
	// signatureEngine = Signature.getInstance("SHA1withRSA");
	// signatureEngine.initSign(privateKey, new SecureRandom());
	//
	// }

	@Override
	public void run() {
		TimeUtility wovTimer = new TimeUtility(wovDuration);

		TimeUtility methodTimer = new TimeUtility();

		byte[] receiveData;
		try {
			receiveData = new byte[this.prsSocket.getReceiveBufferSize()];
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			receiveData = new byte[114688];
		}

		while (true) {
			// Run the payload applications here
			Set<Integer> clientIds = new HashSet<Integer>();

			clientIds.add(0);

			Server server = new Server(2, 5, 10000, prsId, clientIds,
					correctState);

			new Thread(server).start();

			println("Server started successfully");

			// wait for the wov timer to expire
			while (wovTimer.timerHasNotExpired())
				;

			println("the window of vulnerability ended. Starting rejuvenation procedure ...");

			methodTimer.reset();
			// tu.reset();
			// // Wait for some delta time
			// while (tu.timerHasNotExpired())
			// ;

			// Now start the state recovery process
			PRSServerSocketListener pssl = new PRSServerSocketListener();
			Thread psslThread = new Thread(pssl);
			psslThread.start();

			// Wait for a 2*delta time or for other PRWs to be ready
			TimeUtility twoDelta = new TimeUtility(2 * 5000);
			twoDelta.reset();
			while (twoDelta.timerHasNotExpired())
				;

			println("Delta waiting ended");

			// Send a dump message to payload server
			long nonce = sendDumpMessage();

			// Wait for the state to arrive
			TimeUtility stateTimer = new TimeUtility(30000);
			stateTimer.reset();
			this.shareDB = null;
			while (true && stateTimer.timerHasNotExpired()) {
				DatagramPacket receivePacket = new DatagramPacket(receiveData,
						receiveData.length);
				try {
					this.payloadSocket.receive(receivePacket);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				PRSMessage pMessage = (PRSMessage) SerializationUtil
						.deserialize(receivePacket.getData());
				if (pMessage.getType().equals(
						PRSMessageType.DUMP_STATE_RESPONSE)
						&& nonce == pMessage.getNonce()) {
					if (skm.verifyPRSSignature(pMessage)) {
						// Got a response from the payload server
						DumpStateResponseMessage dsrm = (DumpStateResponseMessage) SerializationUtil
								.deserialize(pMessage.getSerializedMessage());

						// Store the state in temporary variable
						this.shareDB = dsrm.getShareDB();
						println("Received state from payload with size "
								+ shareDB.size());
						break;

					}
				}

			}

			// if(shareDB != null){
			// //Didnot receive the state from payload
			// //Perhaps the payload is compromised
			//
			//
			//
			// }

			println("< ------------- STATE RECOVERY PHASE ------------->");
			// Now send the payload state to all other PRWs
			// NOte that the shareDB might be null
			SendStateMessage ssm = new SendStateMessage(this.shareDB,
					this.getPrsId());
			sendMessageToAllServers(ssm, PRSMessageType.SEND_STATE);

			// Now wait for all other PRWs to send their state
			Set<SendStateMessage> ssmSet = new HashSet<SendStateMessage>();
			stateTimer = new TimeUtility(stateTimer.getLocaltimeOut()+twoDelta.getLocaltimeOut());
			stateTimer.reset();
			int messagesReceived = 0;
			while (stateTimer.timerHasNotExpired()) {
				try {
					if (messagesReceived == l)
						break;
					PRSServerMessage psmtemp = serverMessages.poll(
							TimeUtility.timeOut, TimeUnit.MILLISECONDS);
					if (psmtemp == null) {
						println("No message received. Timing out");
						break;
					}
					if (psmtemp.getType().equals(PRSMessageType.SEND_STATE)) {
						SendStateMessage ssmtemp = (SendStateMessage) SerializationUtil
								.deserialize(psmtemp.getSerializedMessage());
						println("Received state from PRW "
								+ ssmtemp.getSenderId() + " with size "
								+ ssmtemp.getShareDB().size());
						System.out.println(ssmtemp.getShareDB());
						ssmSet.add(ssmtemp);
						messagesReceived++;
					}

				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

			int nullstatecount = 0;
			HashMap<String, TempState> temp = new HashMap<String, ProactiveRecoveryServer.TempState>();
			if (ssmSet.size() == l) {
				// Received l states

				// Iterate over the received states
				for (SendStateMessage tempssm : ssmSet) {
					if (tempssm.getShareDB() != null) {
						// Iterate over the shares in a single state
						for (String key : tempssm.getShareDB().keySet()) {
							SecretShare ss = tempssm.getShareDB().get(key);
							println(key + " - " + ss);
							try {
								MessageDigest md = MessageDigest
										.getInstance("SHA");
								String digestStr = new String(
										md.digest(new String(key + "-"
												+ ss.toString()).getBytes()));
								TempState ts = temp.get(digestStr);
								if (ts == null) {
									ts = new TempState(1, key, ss);
									temp.put(digestStr, ts);
								} else {
									ts.setCount(ts.getCount() + 1);
									temp.put(digestStr, ts);
								}
							} catch (NoSuchAlgorithmException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}

						}
					}else{
						nullstatecount++;
						if(nullstatecount > t){
							//Received more than threshold null states
							notifyadmin("Received more than threshold null states");
						}
					}
				}

			}

			correctState = new HashMap<String, SecretShare>();
			// Now get the shares with at least 2t+1 responses and create a new
			// correct state
			for (String key : temp.keySet()) {
				TempState ts = temp.get(key);
				if (ts.getCount() > 2 * t) {
					println("Adding name " + ts.getName() + " in new state");
					correctState.put(ts.getName(), ts.getShare());
				}
			}

			// Now the correct state is recreated
			println("Correct State size : " + correctState.size());
			// for (String s : correctState.keySet()) {
			// println(s + " : " + correctState.get(s));
			// }

			// Stage 3 : Share Renewal phase starts here
			println("< ------------- SHARE RENEWAL PHASE ------------->");

			// Now generate l secrets with new Poly with constant factor as 0
			BigInteger[] secrets = stkm.generateSecrets();
			println("Secrets generated successfully");

			// Send secrets[i] to server i
			for (int i = 0; i < l; i++) {
				InternalPRSServerMessage ssmessage = new SendSecretMessage(
						secrets[i], this.getPrsId());
				ssmessage.setDestinationId(i);
				PRSServerMessage psm = new PRSServerMessage(
						new Random().nextLong(), getPrsId(),
						SerializationUtil.serialize(ssmessage),
						PRSMessageType.SEND_SECRET);
				sendMessage(psm, i);
			}

			// Now wait for all other PRWs to send their secrets
			Set<BigInteger> secretSet = new HashSet<BigInteger>();
			stateTimer.reset();
			messagesReceived = 0;
			while (stateTimer.timerHasNotExpired()) {
				try {
					if (messagesReceived == l)
						break;
					PRSServerMessage psmtemp = serverMessages.poll(
							TimeUtility.timeOut, TimeUnit.MILLISECONDS);
					if (psmtemp == null) {
						println("No message received. Timing out");
						break;
					}
					if (psmtemp.getType().equals(PRSMessageType.SEND_SECRET)) {
						SendSecretMessage ssmtemp = (SendSecretMessage) SerializationUtil
								.deserialize(psmtemp.getSerializedMessage());
						secretSet.add(ssmtemp.getSecret());
						messagesReceived++;
					}

				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			
			if(messagesReceived < l){
				notifyadmin("Didnt receive the required number of secrets in renewal phase");
			}

			// Received all the secrets from other PRWs
			// Update the current secret using these secrets
			BigInteger verifier = stkm.updateKeyshare(secretSet);
			println("Key Share updated successfully");

			// Now send the updated verifier to all the other PRWs

			InternalPRSServerMessage svmessage = new SendVerifierMessage(
					verifier, this.getPrsId());
			nonce = sendMessageToAllServers(svmessage,
					PRSMessageType.SEND_VERIFIER);

			// Now wait for all other PRWs to send their verifiers
			BigInteger verifierSet[] = new BigInteger[l];
			stateTimer.reset();
			messagesReceived = 0;
			while (stateTimer.timerHasNotExpired()) {
				try {
					if (messagesReceived == l && checkArray(verifierSet))
						break;
					PRSServerMessage psmtemp = serverMessages.poll(
							TimeUtility.timeOut, TimeUnit.MILLISECONDS);
					if (psmtemp == null) {
						println("No message received. Timing out");
						break;
					}
					if (psmtemp.getType().equals(PRSMessageType.SEND_VERIFIER)) {
						SendVerifierMessage svmtemp = (SendVerifierMessage) SerializationUtil
								.deserialize(psmtemp.getSerializedMessage());
						verifierSet[svmtemp.getSenderId()] = svmtemp
								.getVerifier();
						messagesReceived++;
					}

				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

			if(messagesReceived < l){
				notifyadmin("Didnt receive the required number of verifiers in renewal phase");
			}else if(!checkArray(verifierSet)){
				notifyadmin("Received null verifiers in renewal phase");
			}
			
			// Now that we have received verfiers from other PRWs, update the
			// keyshares and group keys
			stkm.updateVerifiers(verifierSet);
			println("Verifiers updated successfully");

			println("Rejuvenation procedure successfully executed in "
					+ methodTimer.delta() + " ms");

			wovTimer.reset();
			stateTimer.reset();
		}
	}

	private void notifyadmin(String string) {
		// TODO Auto-generated method stub
		System.out.println(string);
		System.exit(0);
	}

	public boolean checkArray(Object[] array) {
		for (Object o : array) {
			if (o == null)
				return false;
		}
		return true;
	}

	public long sendDumpMessage() {
		DumpStateMessage dsm = new DumpStateMessage();
		PRSMessage pmessage = new PRSMessage(new Random().nextLong(),
				PRSMessageType.DUMP_STATE, SerializationUtil.serialize(dsm));

		skm.signMessage(pmessage);

		// pmessage.setSerializedMessageSignature(signMessage(pmessage
		// .getSerializedMessage()));

		byte[] data = SerializationUtil.serialize(pmessage);
		DatagramPacket dp = new DatagramPacket(data, data.length,
				payloadConnectionInfo.getInetAddress(),
				payloadConnectionInfo.getPort());

		try {
			this.payloadSocket.send(dp);
			println("DUMP Message Sent to Server " + prsId + " at port "
					+ payloadConnectionInfo.getPort());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return pmessage.getNonce();
	}

	public void printState() {
		for (String s : shareDB.keySet()) {
			println(s + " : " + shareDB.get(s));
		}
	}

	public long sendMessageToAllServers(InternalPRSServerMessage ipsm,
			PRSMessageType pmt) {
		println("Sending " + pmt + " message to " + l + " servers");
		// Clear the server messages queue
		// serverMessages.clear();

		if (ipsm == null || pmt == null) {
			return -1;
		}

		long nonce = new Random().nextLong();

		for (int i = 0; i < l; i++) {
			ipsm.setDestinationId(i);
			PRSServerMessage psm = new PRSServerMessage(nonce, getPrsId(),
					SerializationUtil.serialize(ipsm), pmt);
			sendMessage(psm, i);
		}
		return nonce;
	}

	// public byte[] signMessage(byte[] data) {
	// try {
	// signatureEngine.update(data);
	// return signatureEngine.sign();
	// } catch (SignatureException e) {
	// e.printStackTrace();
	// }
	// return null;
	//
	// }

	private void sendMessage(PRSServerMessage psm, int serverId) {
		println("Sending message of type " + psm.getType() + " to server "
				+ serverId);
		if (psm.getSerializedMessage() == null) {
			return;
		}

		psm.setSenderId(getPrsId());

		// produce signature
		skm.signMessage(psm);

		byte[] dataToSend = SerializationUtil.serialize(psm);

		DatagramPacket dp = new DatagramPacket(dataToSend, dataToSend.length,
				serverConnectionInfo.get(serverId).getInetAddress(),
				serverConnectionInfo.get(serverId).getPort());

		try {
			prsSocket.send(dp);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public int getPrsId() {
		return prsId;
	}

	public void setPrsId(int prsId) {
		this.prsId = prsId;
	}

	private void println(Object string) {
		long temptimer = System.currentTimeMillis();
		// System.out.println("<"+dateFormatter.format(new Date())+">"+"Server "
		// + this.getServerId() + " : " + string);
		System.out.println("<" + (temptimer - timer) + ">" + "PRS Server "
				+ this.getPrsId() + " : " + string);
		timer = temptimer;

	}

	private class PRSServerSocketListener implements Runnable {

		@Override
		public void run() {
			// TODO Auto-generated method stub

			byte[] buf;
			try {
				buf = new byte[prsSocket.getReceiveBufferSize()];
			} catch (SocketException e1) {
				e1.printStackTrace();
				buf = new byte[114688];
			}
			DatagramPacket packet = new DatagramPacket(buf, buf.length);

			while (true) {

				try {
					println("Waiting for PRS server messages on socket : "
							+ prsSocket.getLocalPort());
					prsSocket.receive(packet);

					handlePRSServerPacket(packet);

				} catch (IOException e) {
					e.printStackTrace();
				}
			}

		}

		private void handlePRSServerPacket(DatagramPacket receivePacket) {
			PRSServerMessage psm = (PRSServerMessage) SerializationUtil
					.deserialize(receivePacket.getData());

			println("Received " + psm.getType() + " from server "
					+ psm.getSenderId() + " with nonce " + psm.getNonce());

			// Verify the server signature and return if not valid
			if (!skm.verifyServerSignature(psm)) {
				System.out
						.println("Cannot verify signature from server message");
				return;
			}

			if (psm.getType().equals(PRSMessageType.SEND_STATE)
					|| psm.getType().equals(PRSMessageType.SEND_SECRET)
					|| psm.getType().equals(PRSMessageType.SEND_VERIFIER)) {
				println("Added a PRS_SERVER_MESSAGE:" + psm.getType()
						+ " in message queue with nonce " + psm.getNonce());
				serverMessages.add(psm);
			}

		}
	}

	private class TempState {
		int count = 0;
		String name;
		SecretShare share;

		public TempState(int count, String name, SecretShare share) {
			super();
			this.count = count;
			this.name = name;
			this.share = share;
		}

		public int getCount() {
			return count;
		}

		public void setCount(int count) {
			this.count = count;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public SecretShare getShare() {
			return share;
		}

		public void setShare(SecretShare share) {
			this.share = share;
		}

	}

}
