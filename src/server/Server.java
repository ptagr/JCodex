package server;

import java.io.IOException;
import java.math.BigInteger;
import java.net.BindException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import proactive.messages.DumpStateResponseMessage;
import proactive.messages.PRSMessage;
import proactive.messages.PRSMessageType;

import main.ConnectionInfo;
import main.Constants;
import main.FileOperations;
import server.messages.BlindedReadResponse;
import server.messages.CODEXServerMessage;
import server.messages.CODEXServerMessageType;
import server.messages.ForwardReadRequestAccept;
import server.messages.InternalServerMessage;
import server.messages.SignReadResponseRequest;
import server.messages.SignTimeStampResponseRequest;
import server.messages.SignUpdateRejectReponse;
import server.messages.SignUpdateAcceptResponse;
import server.messages.SignedReadResponse;
import server.messages.SignedTimeStampResponse;
import server.messages.SignedUpdateRejectResponse;
import server.messages.SignedUpdateAcceptResponse;
import server.messages.TimeStampReadResponse;
import server.messages.TimeStampRequest;
import server.messages.TimeStampResponse;
import server.messages.UpdateAcceptResponse;
import server.messages.UpdateRejectResponse;
import server.messages.UpdateRequest;
import server.messages.ClientUpdateRejectResponse;
import server.messages.ClientUpdateAcceptResponse;
import state.StateManager;
import threshsig.SigShare;
import utils.SerializationUtil;
import utils.TimeUtility;
import client.messages.CODEXClientMessage;
import client.messages.CODEXClientMessageType;
import client.messages.ClientReadRequest;
import client.messages.ClientTimeStampRequest;
import client.messages.ClientUpdateRequest;

public class Server implements Runnable {
	private int l;
	private int t;

	int quorumSize;

	// public static String broadcastIP = "230.0.0.1";

	public static int baseServerPort = 7000;

	public static int prsPort = 6000;
	public static int timeOutForAcceptanceMessages = 10000; // time in mSec

	private int serverId;

	private ServerKeyManager skm;

	private ServerThresholdKeyManager stkm;

	// private DateFormat dateFormatter = new SimpleDateFormat("ss:S");

	private long timer = 0;

	// The thread listening to incoming messages on the broadcast channel
	private Thread bclThread;
	
	private Thread prsThread;

	private StateManager stateManager;

	private Map<Integer, ConnectionInfo> serverConnectionInfo;

	private DatagramSocket clientConnectionSocket;

	private DatagramSocket prsConnectionSocket;

	private DatagramSocket serverConnectionSocket;

	/* This will be set by a PRS message */
	private volatile boolean stopAcceptingNewClientMessages = false;

	private volatile PRSMessage dumpMessage = null;

	// ReentrantLock messageCacheLock = new ReentrantLock();
	//
	// private LinkedHashMap<Long, Set<CODEXServerMessage>> messageCache = new
	// AccessOrderCache<Long, Set<CODEXServerMessage>>(
	// 50);

	// private LinkedHashMap<Long, Object> serverCheckCache = new
	// AccessOrderCache<Long, Object>(
	// 50);

	LinkedBlockingQueue<CODEXServerMessage> serverMessages = new LinkedBlockingQueue<CODEXServerMessage>(
			10);

	LinkedBlockingQueue<CODEXClientMessage> clientMessages = new LinkedBlockingQueue<CODEXClientMessage>();

	LinkedBlockingQueue<Long> clientMessagesProcessed = new LinkedBlockingQueue<Long>();

	public Server(int k, int l, int clientPort, int serverId,
			Set<Integer> clientIds) {
		this.t = k - 1;
		this.l = l;

		this.quorumSize = 3 * t + 1;

		this.serverId = serverId;
		
		this.stateManager = new StateManager(serverId);

		this.skm = new ServerKeyManager(clientIds, serverId, l);

		this.stkm = new ServerThresholdKeyManager(serverId);

		this.prsThread = new Thread(new PRSSocketListener());
		prsThread.start();
		
		// INitialize and start the listener
		this.bclThread = new Thread(new ServerSocketListener());
		bclThread.start();
		
		

		try {
			this.clientConnectionSocket = new DatagramSocket(clientPort
					+ serverId);
			this.clientConnectionSocket.setSoTimeout(1000);
			// this.serverSocket.setSoTimeout(TimeUtility.timeOut);
		} catch (SocketException e) {

			if (e instanceof BindException) {
				println("Port already in use");
			}

			e.printStackTrace();
		}

		serverConnectionInfo = new HashMap<Integer, ConnectionInfo>();

		// Add connections for all servers except one that matches the id
		// connection)
		for (int i = 0; i < l; i++) {
			// if (i != serverId)
			serverConnectionInfo.put(i, new ConnectionInfo("localhost",
					baseServerPort + i));
		}
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		byte[] receiveData = new byte[16384];
		TimeUtility tu = new TimeUtility();
		println("Waiting for client messages on socket : "
				+ clientConnectionSocket.getLocalPort());
		while (true) {
			DatagramPacket receivePacket = new DatagramPacket(receiveData,
					receiveData.length);
			try {

				if (clientMessages.peek() != null) {
					CODEXClientMessage ccm = clientMessages.poll();
					println("Handling client message from queue sent by client "
							+ ccm.getSenderId()
							+ " with nonce "
							+ ccm.getNonce());
					handleClientMessage(ccm);
				} else {
					if (!stopAcceptingNewClientMessages) {
						this.clientConnectionSocket.receive(receivePacket);
						tu.reset();
						handlePacket(receivePacket);
						System.out.println("Handled packet in " + tu.delta()
								+ " ms");
					} else {
						// Have processed all pending client messages in queue
						// Now time to dump state
						println("Preparing to dump state");
						// Send a reply back to the PRS
						DumpStateResponseMessage dsrm = new DumpStateResponseMessage(
								dumpMessage, stateManager.dumpServerState());

						PRSMessage pmessage = new PRSMessage(
								dumpMessage.getNonce(),
								PRSMessageType.DUMP_STATE_RESPONSE,
								SerializationUtil.serialize(dsrm));

						skm.signMessage(pmessage);

						byte[] data = SerializationUtil.serialize(pmessage);
						this.prsConnectionSocket.send(new DatagramPacket(data,
								data.length, dumpMessage.getAddress(),
								dumpMessage.getPort()));

						//this.bclThread.
						break;
					}
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				if (!(e instanceof SocketTimeoutException))
					e.printStackTrace();
			}
		}

		
		println("Not accepting messages now");
	}

	/*
	 * Function to dump server state
	 */
	

	private void handlePacket(DatagramPacket receivePacket) {
		CODEXClientMessage cm = (CODEXClientMessage) SerializationUtil
				.deserialize(receivePacket.getData());
		cm.setAddress(receivePacket.getAddress());
		cm.setPort(receivePacket.getPort());
		handleClientMessage(cm);
	}

	private void handleClientMessage(CODEXClientMessage cm) {

		// System.out.println("Received a client request");

		TimeUtility tu = new TimeUtility();
		println("Received " + cm.getType() + " from client " + cm.getSenderId()
				+ " with nonce " + cm.getNonce());

		// Verify the client signature and return if not valid
		if (!skm.verifyClientSignature(cm)) {
			println("Cannot verify signature");
			return;
		}
		if (cm.getType()
				.equals(CODEXClientMessageType.CLIENT_TIMESTAMP_REQUEST)) {

			ClientTimeStampRequest ctr = (ClientTimeStampRequest) SerializationUtil
					.deserialize(cm.getSerializedMessage());

			// Request a timestamp from a quorum of servers
			TimeStampRequest tsr = new TimeStampRequest(cm);

			// CODEXServerMessage csm_tsr = new CODEXServerMessage(
			// new Random().nextLong(), getServerId(),
			// CODEXServerMessageType.FORWARD_TIMESTAMP_REQUEST,
			// SerializationUtil.serialize(tsr));

			long nonce_ftr = sendMessageToAllServers(tsr,
					CODEXServerMessageType.TIMESTAMP_REQUEST);

			// Now wait for responses of type FORWARD_TIMESTAMP_RESPONSE from a
			// quorum
			// of servers
			// Now wait for a quorum number of messages
			HashMap<BigInteger, Set<CODEXServerMessage>> evidenceMap = new HashMap<BigInteger, Set<CODEXServerMessage>>();
			// /Set<CODEXServerMessage> rejectSet = new
			// HashSet<CODEXServerMessage>();
			tu.reset();
			int messagesReceived = 0;
			while (tu.timerHasNotExpired()) {
				CODEXServerMessage csmtemp;
				try {
					if (messagesReceived == quorumSize)
						break;
					csmtemp = serverMessages.poll(TimeUtility.timeOut,
							TimeUnit.MILLISECONDS);
					if (csmtemp == null) {
						println("No message received. Timing out");
						break;
					}
					if (nonce_ftr == csmtemp.getNonce()) {
						if (csmtemp.getType().equals(
								CODEXServerMessageType.TIMESTAMP_RESPONSE)) {

							TimeStampResponse tsres = (TimeStampResponse) SerializationUtil
									.deserialize(csmtemp.getSerializedMessage());

							// Check whether tsres.getCcm() == cm
							// Later

							Set<CODEXServerMessage> tempevidenceSet = evidenceMap
									.get(tsres.getTimeStamp());
							if (tempevidenceSet == null) {
								tempevidenceSet = new HashSet<CODEXServerMessage>();
								evidenceMap.put(tsres.getTimeStamp(),
										tempevidenceSet);
							}

							tempevidenceSet.add(csmtemp);

							messagesReceived++;

						}

					}

				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}

			// There might be multiple sets with t+1 entries
			// This might happen because the compromised servers might team
			// up
			// with servers with old state to send the old timestamp
			// To break the tie, select the set with higher TimeStamp
			Set<CODEXServerMessage> evidenceSetTS = null;
			BigInteger correctTimeStamp = null;
			for (BigInteger key : evidenceMap.keySet()) {
				Set<CODEXServerMessage> tsSet = evidenceMap.get(key);
				if (tsSet.size() >= t + 1) {
					if (evidenceSetTS == null
							|| (correctTimeStamp != null && key
									.compareTo(correctTimeStamp) == 1)) {
						evidenceSetTS = tsSet;
						correctTimeStamp = key;
					}
				}
			}

			if (evidenceSetTS == null) {
				// Handle this later
				// No timestamp found
				println("EvidenceSet for TimestampResponse is null");
				println("I should be compromised");
			}

			println("MAJORITY : Got " + evidenceSetTS.size()
					+ " messages with timestamp " + correctTimeStamp);
			// REceived t+1 pieces of evidence

			TimeStampReadResponse trr = new TimeStampReadResponse(cm,
					correctTimeStamp, ctr.getDataId());

			// Create the sign request
			SignTimeStampResponseRequest strr = new SignTimeStampResponseRequest(
					evidenceSetTS, trr);

			// CODEXServerMessage csm2 = new CODEXServerMessage(
			// new Random().nextLong(), getServerId(),
			// CODEXServerMessageType.SIGN_TIMESTAMP_ACCEPT_RESPONSE,
			// SerializationUtil.serialize(strr));

			// Invoke a threshold signature protocol with all servers
			// to sign the response message
			// sendMessageToAllServers(csm2);
			long nonce_strr = sendMessageToAllServers(strr,
					CODEXServerMessageType.SIGN_TIMESTAMP_ACCEPT_RESPONSE);

			BigInteger digitalSigRes = null;
			Set<SigShare> sigs = new HashSet<SigShare>();
			byte[] trrBytes = SerializationUtil.serialize(trr);
			tu.reset();
			while (tu.timerHasNotExpired()) {
				CODEXServerMessage csmtemp;
				try {
					csmtemp = serverMessages.poll(TimeUtility.timeOut,
							TimeUnit.MILLISECONDS);

					if (csmtemp == null) {
						println("No message received. Timing out");
						break;
					}
					if (nonce_strr == csmtemp.getNonce()) {
						if (csmtemp
								.getType()
								.equals(CODEXServerMessageType.SIGNED_TIMESTAMP_ACCEPT_RESPONSE)) {
							SignedTimeStampResponse str = (SignedTimeStampResponse) SerializationUtil
									.deserialize(csmtemp.getSerializedMessage());

							// Verify the signature on blinded response
							if (stkm.verifySignedShare(trrBytes,
									str.getSignedShare())) {
								sigs.add(str.getSignedShare());
								if (sigs.size() >= (t + 1)) {
									println("Got t+1 signatures : "
											+ sigs.size());
									// Try generating the threshold
									// signature
									// Use the partial signatures to create
									// the
									// signed
									// response for the client
									digitalSigRes = stkm.thresholdSign(
											trrBytes, sigs);
									if (digitalSigRes == null) {
										// Signature could not be generated
										// So continue the loop and try one
										// more
										// signature
									} else {
										break;
									}

								}

							} else {
								println("Cannot verify timestamp response");
							}

						}
					}

				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}

			if (digitalSigRes == null) {
				// Could not generate threshold signature
				// Handle this later
				error("Could not generate a valid threshold signature");
			} else {

				CODEXClientMessage clientRes = new CODEXClientMessage(
						cm.getNonce(), getServerId(),
						CODEXClientMessageType.TIMESTAMP_RESPONSE, trrBytes);
				clientRes.setSerializedMessageSignature(digitalSigRes
						.toByteArray());

				// Send back response
				println("Send back client reponse on host " + cm.getAddress()
						+ " and port " + cm.getPort());
				sendResponseMessage(clientRes, cm.getAddress(), cm.getPort());
			}

		} else if (cm.getType().equals(
				CODEXClientMessageType.CLIENT_READ_REQUEST)) {
			ClientReadRequest crr = (ClientReadRequest) SerializationUtil
					.deserialize(cm.getSerializedMessage());
			// println(crr.getEncryptedBlindingFactor().toString());

			CODEXServerMessage csm = new CODEXServerMessage(
					new Random().nextLong(), getServerId(),
					CODEXServerMessageType.READ_REQUEST,
					SerializationUtil.serialize(cm));

			println("Forwarding CLIENT_READ_REQUEST message to " + l
					+ " servers");

			// Make an entry in serverCheckCache of <nonce, MR(n)>
			// serverCheckCache.put(csm.getNonce(), cm);

			// Send messages to all servers
			sendMessageToAllServers(csm);

			// Now wait for a quorum number of messages
			HashMap<SecretShare, Set<ForwardReadRequestAccept>> evidenceMap = new HashMap<SecretShare, Set<ForwardReadRequestAccept>>();
			tu.reset();
			int messagesReceived = 0;
			while (tu.timerHasNotExpired()) {
				CODEXServerMessage csmtemp;
				try {
					if (messagesReceived == quorumSize)
						break;
					csmtemp = serverMessages.poll(TimeUtility.timeOut,
							TimeUnit.MILLISECONDS);
					if (csmtemp == null) {
						println("No message received. Timing out");
						break;
					}
					if (csmtemp.getType().equals(
							CODEXServerMessageType.READ_ACCEPT_RESPONSE)) {
						ForwardReadRequestAccept frra = (ForwardReadRequestAccept) SerializationUtil
								.deserialize(csmtemp.getSerializedMessage());

						// Now check the signature of MR(n)
						if (skm.verifyServerSignature(SerializationUtil
								.serialize(cm), frra.getDigitalSig()
								.toByteArray(), csmtemp.getSenderId())) {

							// Now check the validity of the decrypted partial
							// share
							if (stkm.verifyDecryptedShare(frra.getCipher()
									.toByteArray(), frra.getDecryptedShare())) {
								SecretShare key = new SecretShare(
										frra.getTimeStamp(), frra.getCipher());
								Set<ForwardReadRequestAccept> tempevidenceSet = evidenceMap
										.get(key);
								if (tempevidenceSet == null) {
									tempevidenceSet = new HashSet<ForwardReadRequestAccept>();
									evidenceMap.put(key, tempevidenceSet);
								}

								tempevidenceSet.add(frra);
								// if (tempevidenceSet.size() == t+1) {
								// evidenceSet = tempevidenceSet;
								// correctCipher = frra.getCipher();
								// break;
								// }

								messagesReceived++;

							}
						}
					}
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}

			// There might be multiple sets with t+1 entries
			// This might happen because the compromised servers might team up
			// with servers with old state to send the old timestamp
			// To break the tie, select the set with higher TimeStamp
			Set<ForwardReadRequestAccept> evidenceSet = null;
			BigInteger correctCipher = null;
			BigInteger correctTimeStamp = null;
			for (SecretShare key : evidenceMap.keySet()) {
				Set<ForwardReadRequestAccept> frraSet = evidenceMap.get(key);
				if (frraSet.size() >= t + 1) {
					if (evidenceSet == null
							|| (correctTimeStamp != null && key.getTimestamp()
									.compareTo(correctTimeStamp) == 1)) {
						evidenceSet = frraSet;
						correctCipher = key.getSecret();
						correctTimeStamp = key.getTimestamp();
					}
				}
			}

			if (evidenceSet == null) {
				// Handle this later
				System.out.println("Evidence set null");
			} else {
				println("MAJORITY : Got " + evidenceSet.size()
						+ " messages with timestamp " + correctTimeStamp);
				// REceived t+1 pieces of evidence

				// Get t+1 sigshares to decrypt cipher
				Set<SigShare> shares = new HashSet<SigShare>();

				for (ForwardReadRequestAccept frra : evidenceSet) {
					// if (frra.getDecryptedShare().getId() == 2
					// || frra.getDecryptedShare().getId() == 4)
					// shares[i++] = frra.getDecryptedShare();
					shares.add(frra.getDecryptedShare());
				}

				BigInteger blindedSecret = stkm.thresholdDecrypt(correctCipher,
						shares);

				// Create the unsigned server response to be sent back to
				// the client
				BlindedReadResponse brr = new BlindedReadResponse(crr,
						blindedSecret, crr.getDataId());

				// Create the sign request
				SignReadResponseRequest srrr = new SignReadResponseRequest(
						evidenceSet, brr);

				CODEXServerMessage csm2 = new CODEXServerMessage(
						new Random().nextLong(), getServerId(),
						CODEXServerMessageType.SIGN_READ_ACCEPT_RESPONSE,
						SerializationUtil.serialize(srrr));

				// Invoke a threshold signature protocol with all servers
				// to sign the response message
				sendMessageToAllServers(csm2);

				BigInteger digitalSigRes = null;
				Set<SigShare> sigs = new HashSet<SigShare>();
				byte[] brrBytes = SerializationUtil.serialize(brr);
				tu.reset();
				while (tu.timerHasNotExpired()) {
					CODEXServerMessage csmtemp;
					try {
						csmtemp = serverMessages.poll(TimeUtility.timeOut,
								TimeUnit.MILLISECONDS);

						if (csmtemp == null) {
							println("No message received. Timing out");
							break;
						}

						if (csmtemp
								.getType()
								.equals(CODEXServerMessageType.SIGNED_READ_ACCEPT_RESPONSE)
								&& isNonceEqual(csm2, csmtemp)) {
							SignedReadResponse srr = (SignedReadResponse) SerializationUtil
									.deserialize(csmtemp.getSerializedMessage());

							// Verify the signature on blinded response
							if (stkm.verifySignedShare(brrBytes,
									srr.getSignedShare())) {
								sigs.add(srr.getSignedShare());
								if (sigs.size() >= (t + 1)) {
									println("Got t+1 signatures : "
											+ sigs.size());
									// Try generating the threshold signature
									// Use the partial signatures to create the
									// signed
									// response for the client
									digitalSigRes = stkm.thresholdSign(
											brrBytes, sigs);
									if (digitalSigRes == null) {
										// Signature could not be generated
										// So continue the loop and try one more
										// signature
									} else {
										break;
									}

								}

							} else {
								println("Cannot verify blinded response");
							}

						}

						// //
						// println("Digital Signature on response message : "+digitalSigRes);
						//
						// // Verify the digital signature just in case
						// if (!stkm.verifySignature(brrBytes, digitalSigRes)) {
						// // Panic mode
						// // The threshold generated signature on the response
						// // message is incorrect
						// }
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

				}

				if (digitalSigRes == null) {
					// Could not generate threshold signature
					// Handle this later
				} else {

					CODEXClientMessage clientRes = new CODEXClientMessage(
							cm.getNonce(), getServerId(),
							CODEXClientMessageType.BLINDED_READ_RESPONSE,
							brrBytes);
					clientRes.setSerializedMessageSignature(digitalSigRes
							.toByteArray());

					// Send back response
					println("Send back client reponse on host "
							+ cm.getAddress() + " and port " + cm.getPort());
					sendResponseMessage(clientRes, cm.getAddress(),
							cm.getPort());
				}

			}

		} else if (cm.getType().equals(
				CODEXClientMessageType.CLIENT_UPDATE_REQUEST)) {

			// Add the request to receivedMEssage history
			clientMessagesProcessed.add(cm.getNonce());

			// Write Protocol starts here
			ClientUpdateRequest crw = (ClientUpdateRequest) SerializationUtil
					.deserialize(cm.getSerializedMessage());
			// println(crr.getEncryptedBlindingFactor().toString());

			// First get the CODEXClientMessage from the write request and
			// check its signature and other details
			CODEXClientMessage ccm = crw.getCcm();
			if (!ccm.getType()
					.equals(CODEXClientMessageType.TIMESTAMP_RESPONSE)
					|| !stkm.verifySignature(ccm)) {
				// Then check failed
				// Ignore the client request
				println("CHECK for TIMESTAMP_RESPONSE FAILED");
				return;
			}

			// TimeStampReadResponse trr = (TimeStampReadResponse)
			// SerializationUtil.deserialize(ccm.getSerializedMessage());
			// BigInteger correctTimeStamp = trr.getTimestamp();

			UpdateRequest ur = new UpdateRequest(cm);

			long nonce_ur = sendMessageToAllServers(ur,
					CODEXServerMessageType.UPDATE_REQUEST);

			// Set<CODEXServerMessage> csmSet =
			// messageCache.get(csm.getNonce());
			Set<CODEXServerMessage> evidenceSet = new HashSet<CODEXServerMessage>();
			Set<CODEXServerMessage> rejectSet = new HashSet<CODEXServerMessage>();
			// Wait for valid update_accepts from a quorum of servers
			// or update_rejects from (t+1) servers
			// or a timeout to occur
			while (tu.timerHasNotExpired()) {
				CODEXServerMessage csmtemp;
				try {
					csmtemp = serverMessages.poll(TimeUtility.timeOut,
							TimeUnit.MILLISECONDS);

					if (csmtemp == null) {
						println("No message received. Timing out");
						break;
					}
					if (nonce_ur == csmtemp.getNonce()) {
						if (csmtemp.getType().equals(
								CODEXServerMessageType.UPDATE_ACCEPT_RESPONSE)) {

							evidenceSet.add(csmtemp);
							if (evidenceSet.size() == quorumSize) {
								// Received 2t+1 pieces of evidence
								break;
							}

						} else if (csmtemp.getType().equals(
								CODEXServerMessageType.UPDATE_REJECT_RESPONSE)) {
							rejectSet.add(csmtemp);
							if (rejectSet.size() == (t + 1)) {
								// Received t+1 pieces of reject
								break;
							}
						}

					}
				} catch (InterruptedException ie) {
					ie.printStackTrace();
				}
			}

			if (rejectSet.size() == (t + 1)) {
				// Received (t+1) Rejects

				// Create the write reject reponse to be sent to client
				ClientUpdateRejectResponse wrr = new ClientUpdateRejectResponse(
						cm, crw.getDataId());

				// Create the sign request
				SignUpdateRejectReponse swrrr = new SignUpdateRejectReponse(
						rejectSet, wrr);

				// Invoke a threshold signature protocol with all servers
				// to sign the response message
				// sendMessageToAllServers(csm2);
				long nonce_swrrr = sendMessageToAllServers(swrrr,
						CODEXServerMessageType.SIGN_UPDATE_REJECT_RESPONSE);

				BigInteger digitalSigRes = null;
				Set<SigShare> sigs = new HashSet<SigShare>();
				byte[] wrrBytes = SerializationUtil.serialize(wrr);
				tu.reset();
				while (tu.timerHasNotExpired()) {
					CODEXServerMessage csmtemp;
					try {
						csmtemp = serverMessages.poll(TimeUtility.timeOut,
								TimeUnit.MILLISECONDS);

						if (csmtemp == null) {
							println("No message received. Timing out");
							break;
						}
						if (nonce_swrrr == csmtemp.getNonce()) {
							if (csmtemp
									.getType()
									.equals(CODEXServerMessageType.SIGNED_UPDATE_REJECT_RESPONSE)) {
								SignedUpdateRejectResponse swrr = (SignedUpdateRejectResponse) SerializationUtil
										.deserialize(csmtemp
												.getSerializedMessage());

								// Verify the signature on write response
								if (stkm.verifySignedShare(wrrBytes,
										swrr.getSignedShare())) {
									sigs.add(swrr.getSignedShare());
									if (sigs.size() >= (t + 1)) {
										println("Got t+1 signatures : "
												+ sigs.size());
										// Try generating the threshold
										// signature
										// Use the partial signatures to create
										// the
										// signed
										// response for the client
										digitalSigRes = stkm.thresholdSign(
												wrrBytes, sigs);
										if (digitalSigRes == null) {
											// Signature could not be generated
											// So continue the loop and try one
											// more
											// signature
										} else {
											break;
										}

									}

								} else {
									println("Cannot verify timestamp response");
								}

							}
						}

					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

				}

				if (digitalSigRes == null) {
					// Could not generate threshold signature
					// Handle this later
					error("Could not generate a valid threshold signature");
				} else {

					CODEXClientMessage clientRes = new CODEXClientMessage(
							cm.getNonce(), getServerId(),
							CODEXClientMessageType.UPDATE_REJECT_RESPONSE,
							wrrBytes);
					clientRes.setSerializedMessageSignature(digitalSigRes
							.toByteArray());

					// Send back response
					println("Send back client reponse on host "
							+ cm.getAddress() + " and port " + cm.getPort());
					sendResponseMessage(clientRes, cm.getAddress(),
							cm.getPort());
				}
			} else if (evidenceSet.size() >= quorumSize) {
				// Received a quorum pieces of evidence

				// Create the write reponse to be sent to client
				ClientUpdateAcceptResponse wsr = new ClientUpdateAcceptResponse(
						cm, crw.getDataId());

				// Create the sign request
				SignUpdateAcceptResponse swrr = new SignUpdateAcceptResponse(
						evidenceSet, wsr);

				// Invoke a threshold signature protocol with all servers
				// to sign the response message
				// sendMessageToAllServers(csm2);
				long nonce_swrr = sendMessageToAllServers(swrr,
						CODEXServerMessageType.SIGN_UPDATE_ACCEPT_RESPONSE);

				BigInteger digitalSigRes = null;
				Set<SigShare> sigs = new HashSet<SigShare>();
				byte[] wsrBytes = SerializationUtil.serialize(wsr);
				tu.reset();
				while (tu.timerHasNotExpired()) {
					CODEXServerMessage csmtemp;
					try {
						csmtemp = serverMessages.poll(TimeUtility.timeOut,
								TimeUnit.MILLISECONDS);

						if (csmtemp == null) {
							println("No message received. Timing out");
							break;
						}
						if (nonce_swrr == csmtemp.getNonce()) {
							if (csmtemp
									.getType()
									.equals(CODEXServerMessageType.SIGNED_UPDATE_ACCEPT_RESPONSE)) {
								SignedUpdateAcceptResponse swr = (SignedUpdateAcceptResponse) SerializationUtil
										.deserialize(csmtemp
												.getSerializedMessage());

								// Verify the signature on write response
								if (stkm.verifySignedShare(wsrBytes,
										swr.getSignedShare())) {
									sigs.add(swr.getSignedShare());
									if (sigs.size() >= (t + 1)) {
										println("Got t+1 signatures : "
												+ sigs.size());
										// Try generating the threshold
										// signature
										// Use the partial signatures to create
										// the
										// signed
										// response for the client
										digitalSigRes = stkm.thresholdSign(
												wsrBytes, sigs);
										if (digitalSigRes == null) {
											// Signature could not be generated
											// So continue the loop and try one
											// more
											// signature
										} else {
											break;
										}

									}

								} else {
									println("Cannot verify timestamp response");
								}

							}
						}

					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

				}

				if (digitalSigRes == null) {
					// Could not generate threshold signature
					// Handle this later
					error("Could not generate a valid threshold signature");
				} else {

					CODEXClientMessage clientRes = new CODEXClientMessage(
							cm.getNonce(), getServerId(),
							CODEXClientMessageType.UPDATE_ACCEPT_RESPONSE,
							wsrBytes);
					clientRes.setSerializedMessageSignature(digitalSigRes
							.toByteArray());

					// Send back response
					println("Send back client reponse on host "
							+ cm.getAddress() + " and port " + cm.getPort());
					sendResponseMessage(clientRes, cm.getAddress(),
							cm.getPort());
				}
			}

		}
	}

	private void println(String string) {
		long temptimer = System.currentTimeMillis();
		// System.out.println("<"+dateFormatter.format(new Date())+">"+"Server "
		// + this.getServerId() + " : " + string);
		System.out.println("<" + (temptimer - timer) + ">" + "Server "
				+ this.getServerId() + " : " + string);
		timer = temptimer;

	}

	private void error(String string) {
		long temptimer = System.currentTimeMillis();
		// System.out.println("<"+dateFormatter.format(new Date())+">"+"Server "
		// + this.getServerId() + " : " + string);
		System.out.println("<" + (temptimer - timer) + ">" + "ERROR : Server "
				+ this.getServerId() + " : " + string);
		timer = temptimer;

	}

	// public void broadcastMessage(byte[] data) {
	// InetAddress group;
	// try {
	// group = InetAddress.getByName(ReplicaServer.broadcastIP);
	// DatagramPacket packet = new DatagramPacket(data, data.length,
	// group, ReplicaServer.broadcastPort);
	// this.clientConnectionSocket.send(packet);
	// } catch (UnknownHostException e) {
	// // TODO Auto-generated catch block
	// e.printStackTrace();
	// } catch (IOException e) {
	// // TODO Auto-generated catch block
	// e.printStackTrace();
	// }
	//
	// }

	// public boolean isMessageCount(String key, int count) {
	// if (messageCache.containsKey(key)) {
	// if (messageCache.get(key).size() >= count)
	// return true;
	// }
	// return false;
	// }

	public void sendMessageToAllServers(CODEXServerMessage csm) {
		// Clear the server messages queue
		serverMessages.clear();

		if (csm.getSerializedMessage() == null) {
			return;
		}

		// Random r = new Random();
		// Set<Integer> serversToSend = new HashSet<Integer>();
		// while (serversToSend.size() != l) {
		// serversToSend.add(r.nextInt(l));
		// }
		//
		// for (Integer in : serversToSend) {
		// sendMessage(csm, in);
		// }

		for (int i = 0; i < l; i++) {
			sendMessage(csm, i);
		}
		// return serversToSend;
	}

	public long sendMessageToAllServers(InternalServerMessage ism,
			CODEXServerMessageType csmt) {
		println("Sending " + csmt + " message to " + l + " servers");
		// Clear the server messages queue
		serverMessages.clear();

		if (ism == null || csmt == null) {
			return -1;
		}

		long nonce = new Random().nextLong();

		// Random r = new Random();
		// Set<Integer> serversToSend = new HashSet<Integer>();
		// while (serversToSend.size() != l) {
		// serversToSend.add(r.nextInt(l));
		// }
		//
		// for (Integer in : serversToSend) {
		// sendMessage(csm, in);
		// }

		for (int i = 0; i < l; i++) {
			ism.setDestinationId(i);
			CODEXServerMessage csm = new CODEXServerMessage(nonce,
					getServerId(), csmt, SerializationUtil.serialize(ism));
			sendMessage(csm, i);
		}
		return nonce;
	}

	public void sendMessage(CODEXServerMessage csm, int serverId) {
		println("Sending message of type " + csm.getType() + " to server "
				+ serverId);
		if (csm.getSerializedMessage() == null) {
			return;
		}

		csm.setSenderId(getServerId());

		// produce signature
		skm.signMessage(csm);

		byte[] dataToSend = SerializationUtil.serialize(csm);

		DatagramPacket dp = new DatagramPacket(dataToSend, dataToSend.length,
				serverConnectionInfo.get(serverId).getInetAddress(),
				serverConnectionInfo.get(serverId).getPort());
		// Send data over the channel to the delegate
		// ccm.sendMessageSynchronous(dataToSend);

		try {
			serverConnectionSocket.send(dp);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	// public void sendMessage(InternalServerMessage ism, CODEXServerMessageType
	// csmt, long nonce) {
	// println("Sending message of type " + csmt + " to server "
	// + serverId);
	// if (ism == null || csmt == null) {
	// return;
	// }
	//
	// CODEXServerMessage csm = new CODEXServerMessage(nonce, getServerId(),
	// csmt, )
	// csm.setSenderId(getServerId());
	//
	// // produce signature
	// skm.signMessage(csm);
	//
	// byte[] dataToSend = SerializationUtil.serialize(csm);
	//
	// DatagramPacket dp = new DatagramPacket(dataToSend, dataToSend.length,
	// serverConnectionInfo.get(serverId).getInetAddress(),
	// serverConnectionInfo.get(serverId).getPort());
	// // Send data over the channel to the delegate
	// // ccm.sendMessageSynchronous(dataToSend);
	//
	// try {
	// serverConnectionSocket.send(dp);
	// } catch (IOException e) {
	// // TODO Auto-generated catch block
	// e.printStackTrace();
	// }
	// }

	public void sendResponseMessage(CODEXClientMessage ccm, InetAddress add,
			int port) {
		if (ccm.getSerializedMessage() == null
				|| ccm.getSerializedMessageSignature() == null) {
			return;
		}

		ccm.setSenderId(getServerId());

		// not to produce signature since it should already be produced via
		// threshold
		// skm.signMessage(csm);

		byte[] dataToSend = SerializationUtil.serialize(ccm);

		DatagramPacket dp = new DatagramPacket(dataToSend, dataToSend.length,
				add, port);
		// Send data over the channel to the delegate
		// ccm.sendMessageSynchronous(dataToSend);

		try {
			clientConnectionSocket.send(dp);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private class ServerSocketListener implements Runnable {

		@Override
		public void run() {
			// TODO Auto-generated method stub
			println("Running server thread for server " + getServerId());
			try {
				serverConnectionSocket = new DatagramSocket(baseServerPort
						+ getServerId());
				// address = InetAddress.getByName(ReplicaServer.broadcastIP);
				// socket.joinGroup(address);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			printState();

			byte[] buf = new byte[32768];
			DatagramPacket packet = new DatagramPacket(buf, buf.length);

			while (true) {

				try {
					println("Waiting for server messages on socket : "
							+ serverConnectionSocket.getLocalPort());
					serverConnectionSocket.receive(packet);

					handleServerPacket(packet);

					// printState();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

		}

		private void handleServerPacket(DatagramPacket receivePacket) {

			// System.out.println("Received a server message");
			CODEXServerMessage sm = (CODEXServerMessage) SerializationUtil
					.deserialize(receivePacket.getData());

			println("Received " + sm.getType() + " from server "
					+ sm.getSenderId() + " with nonce " + sm.getNonce());

			// Verify the server signature and return if not valid
			if (!skm.verifyServerSignature(sm)) {
				System.out
						.println("Cannot verify signature from server message");
				return;
			}

			if (sm.getType().equals(CODEXServerMessageType.READ_REQUEST)) {
				CODEXClientMessage ccm = (CODEXClientMessage) SerializationUtil
						.deserialize(sm.getSerializedMessage());

				// Verify client message
				if (!skm.verifyClientSignature(ccm)) {
					// The original server should have verified this before
					// sending this message
					// Since it didnt it is assumed compromised

					// Add server to compromised list
				} else {
					// Client Message is correct
					ClientReadRequest crr = (ClientReadRequest) SerializationUtil
							.deserialize(ccm.getSerializedMessage());

					// Check if some value val(n) is locally bound to N
					//SecretShare ss = shareDB.get(crr.getDataId());
					SecretShare ss = stateManager.getSecretShare(crr.getDataId());
					// If no value is bound, ignore the request
					if (ss == null)
						return;

					// Compute blinded ciphertext c = E( val(N) * bp)
					BigInteger cipher = ss.getSecret().multiply(
							crr.getEncryptedBlindingFactor());

					// BigInteger cipher = ss.getSecret();

					// Compute the partial decryption along with the proof of
					// validity
					SigShare decryptedShare = stkm.decrypt(cipher);
					println(decryptedShare.toString());

					// Create the READ_ACCEPT_RESPONSE message to send
					// back
					ForwardReadRequestAccept frra = new ForwardReadRequestAccept(
							new BigInteger(skm.getSignature(ccm)), cipher,
							decryptedShare, ss.getTimestamp());

					CODEXServerMessage csm = new CODEXServerMessage(
							sm.getNonce(), getServerId(),
							CODEXServerMessageType.READ_ACCEPT_RESPONSE,
							SerializationUtil.serialize(frra));

					sendMessage(csm, sm.getSenderId());
				}

			}
			if (sm.getType().equals(CODEXServerMessageType.TIMESTAMP_REQUEST)) {

				TimeStampRequest tsr = (TimeStampRequest) SerializationUtil
						.deserialize(sm.getSerializedMessage());

				if (tsr.getDestinationId() != getServerId()) {
					// Fatal
					// Message was not meant for me
					// Seems to be a replay
					// Handle this later
				}

				CODEXClientMessage ccm = tsr.getCcm();

				// Verify client message
				if (!skm.verifyClientSignature(ccm)) {
					// The original server should have verified this before
					// sending this message
					// Since it didnt it is assumed compromised

					// Add server to compromised list

				} else {
					// Client Message is correct
					ClientTimeStampRequest ctr = (ClientTimeStampRequest) SerializationUtil
							.deserialize(ccm.getSerializedMessage());

					BigInteger timestamp = null;

					// Check if some value is locally bound to N
					//SecretShare ss = shareDB.get(ctr.getDataId());
					SecretShare ss = stateManager.getSecretShare(ctr.getDataId());

					// If no value is bound, then send back a one timestamp
					if (ss == null)
						timestamp = BigInteger.ONE;
					else {
						timestamp = ss.getTimestamp();
					}

					// // Create string with TIMESTAMP_RESPONSE
					// // Timestamp and MW(n)
					// String str = timestamp.toString()
					// + (new BigInteger(SerializationUtil.serialize(ccm))
					// .toString());

					// Create the FORWARD_WRITE_REQUEST_ACCEPT message to send
					// back
					TimeStampResponse tsres = new TimeStampResponse(ccm,
							timestamp, sm.getSenderId());

					CODEXServerMessage csm = new CODEXServerMessage(
							sm.getNonce(), getServerId(),
							CODEXServerMessageType.TIMESTAMP_RESPONSE,
							SerializationUtil.serialize(tsres));

					sendMessage(csm, sm.getSenderId());

				}

			} else if (sm.getType().equals(
					CODEXServerMessageType.SIGN_TIMESTAMP_ACCEPT_RESPONSE)) {

				SignTimeStampResponseRequest strr = (SignTimeStampResponseRequest) SerializationUtil
						.deserialize(sm.getSerializedMessage());

				TimeStampReadResponse trr = strr.getTrr();

				// Check the evidence set
				Set<CODEXServerMessage> evidenceSetTS = strr.getEvidenceSet();
				boolean checkFailed = false;
				if (evidenceSetTS.size() < quorumSize) {
					error("Evidence Set less than quorumSize : "
							+ evidenceSetTS.size() + " < " + quorumSize);
					checkFailed = true;
				} else {

					for (CODEXServerMessage csmtemp : evidenceSetTS) {
						// First verify the signature
						if (skm.verifyServerSignature(csmtemp)) {
							// Next check the type of message
							if (!csmtemp.getType().equals(
									CODEXServerMessageType.TIMESTAMP_RESPONSE)) {
								checkFailed = true;
								error("Message type not as expected : "
										+ CODEXServerMessageType.TIMESTAMP_RESPONSE
										+ " vs " + csmtemp.getType());
								break;
							}

							TimeStampResponse tsr = (TimeStampResponse) SerializationUtil
									.deserialize(csmtemp.getSerializedMessage());

							// CHeck if strr.getTrr().getCcm() == tsr.getCcm()
							if (trr == null || tsr == null) {
								checkFailed = true;
								error("Response to be signed null");
								break;
							}

							if (tsr.getDestinationId() != sm.getSenderId()) {
								checkFailed = true;
								error("Message was meant for "
										+ tsr.getDestinationId() + " and not "
										+ sm.getSenderId());
								break;
							}

							if (trr.getCcm() == null
									|| !trr.getCcm().equals(tsr.getCcm())) {
								error("Client messages dont match");
								checkFailed = true;
								break;
							}

						}
					}
				}

				if (checkFailed) {
					// Dont send anything
					// Add the server to compromised List
					// Ignore future messages from server
					println("Checking of evidence Set failed");
				} else {

					// At this time, the trr will be signed
					SignedTimeStampResponse str = new SignedTimeStampResponse(
							stkm.sign(SerializationUtil.serialize(trr)),
							sm.getSenderId());

					// println("Signed sig : " + srr.getSignedShare());
					// Now create the response to be sent back
					// The serialized message contains the brr
					CODEXServerMessage csm = new CODEXServerMessage(
							sm.getNonce(),
							getServerId(),
							CODEXServerMessageType.SIGNED_TIMESTAMP_ACCEPT_RESPONSE,
							SerializationUtil.serialize(str));

					sendMessage(csm, sm.getSenderId());
				}
				// SigShare SignedRes = stkm.sign(brr);

			} else if (sm.getType().equals(
					CODEXServerMessageType.SIGN_UPDATE_ACCEPT_RESPONSE)) {

				SignUpdateAcceptResponse swrr = (SignUpdateAcceptResponse) SerializationUtil
						.deserialize(sm.getSerializedMessage());

				ClientUpdateAcceptResponse wsr = swrr.getWsr();

				// Check the evidence set
				Set<CODEXServerMessage> evidenceSetTS = swrr.getEvidenceSet();
				boolean checkFailed = false;
				if (evidenceSetTS.size() < quorumSize) {
					error("Evidence Set less than quorumSize : "
							+ evidenceSetTS.size() + " < " + quorumSize);
					checkFailed = true;
				} else {

					for (CODEXServerMessage csmtemp : evidenceSetTS) {
						// First verify the signature
						if (skm.verifyServerSignature(csmtemp)) {
							// Next check the type of message
							if (!csmtemp
									.getType()
									.equals(CODEXServerMessageType.UPDATE_ACCEPT_RESPONSE)) {
								checkFailed = true;
								error("Message type not as expected : "
										+ CODEXServerMessageType.UPDATE_ACCEPT_RESPONSE
										+ " vs " + csmtemp.getType());
								break;
							}

							UpdateAcceptResponse uar = (UpdateAcceptResponse) SerializationUtil
									.deserialize(csmtemp.getSerializedMessage());

							// CHeck if strr.getTrr().getCcm() == tsr.getCcm()
							if (uar == null || wsr == null) {
								checkFailed = true;
								error("Response to be signed null");
								break;
							}

							if (uar.getDestinationId() != sm.getSenderId()) {
								checkFailed = true;
								error("Message was meant for "
										+ uar.getDestinationId() + " and not "
										+ sm.getSenderId());
								break;
							}

							if (uar.getCcm() == null) {
								error("Client message is null");
								checkFailed = true;
								break;
							}

							if (!uar.getCcm().equals(wsr.getCcm())) {
								error("Client messages dont match");
								checkFailed = true;
								break;
							}

						}
					}
				}

				if (checkFailed) {
					// Dont send anything
					// Add the server to compromised List
					// Ignore future messages from server
					println("Checking of evidence Set failed");
				} else {

					// At this time, the wsr will be signed
					SignedUpdateAcceptResponse swr = new SignedUpdateAcceptResponse(
							stkm.sign(SerializationUtil.serialize(wsr)),
							sm.getSenderId());

					// println("Signed sig : " + srr.getSignedShare());
					// Now create the response to be sent back
					// The serialized message contains the brr
					CODEXServerMessage csm = new CODEXServerMessage(
							sm.getNonce(),
							getServerId(),
							CODEXServerMessageType.SIGNED_UPDATE_ACCEPT_RESPONSE,
							SerializationUtil.serialize(swr));

					sendMessage(csm, sm.getSenderId());
				}
				// SigShare SignedRes = stkm.sign(brr);

			} else if (sm.getType().equals(
					CODEXServerMessageType.SIGN_UPDATE_REJECT_RESPONSE)) {

				SignUpdateRejectReponse swrrr = (SignUpdateRejectReponse) SerializationUtil
						.deserialize(sm.getSerializedMessage());

				ClientUpdateRejectResponse wrr = swrrr.getWrr();

				// Check the evidence set
				Set<CODEXServerMessage> evidenceSetTS = swrrr.getEvidenceSet();
				boolean checkFailed = false;
				if (evidenceSetTS.size() < (t + 1)) {
					error("Evidence Set less than (t+1) : "
							+ evidenceSetTS.size() + " < " + (t + 1));
					checkFailed = true;
				} else {

					for (CODEXServerMessage csmtemp : evidenceSetTS) {
						// First verify the signature
						if (skm.verifyServerSignature(csmtemp)) {
							// Next check the type of message
							if (!csmtemp
									.getType()
									.equals(CODEXServerMessageType.UPDATE_REJECT_RESPONSE)) {
								checkFailed = true;
								error("Message type not as expected : "
										+ CODEXServerMessageType.UPDATE_REJECT_RESPONSE
										+ " vs " + csmtemp.getType());
								break;
							}

							UpdateRejectResponse urr = (UpdateRejectResponse) SerializationUtil
									.deserialize(csmtemp.getSerializedMessage());

							// CHeck if strr.getTrr().getCcm() == tsr.getCcm()
							if (urr == null || wrr == null) {
								checkFailed = true;
								error("Response to be signed null");
								break;
							}

							if (urr.getDestinationId() != sm.getSenderId()) {
								checkFailed = true;
								error("Message was meant for "
										+ urr.getDestinationId() + " and not "
										+ sm.getSenderId());
								break;
							}

							if (urr.getCcm() == null
									|| !urr.getCcm().equals(wrr.getCcm())) {
								error("Client messages dont match");
								checkFailed = true;
								break;
							}

						}
					}
				}

				if (checkFailed) {
					// Dont send anything
					// Add the server to compromised List
					// Ignore future messages from server
					println("Checking of evidence Set failed");
				} else {

					// At this time, the wsr will be signed
					SignedUpdateRejectResponse swrr = new SignedUpdateRejectResponse(
							stkm.sign(SerializationUtil.serialize(wrr)),
							sm.getSenderId());

					// println("Signed sig : " + srr.getSignedShare());
					// Now create the response to be sent back
					// The serialized message contains the brr
					CODEXServerMessage csm = new CODEXServerMessage(
							sm.getNonce(),
							getServerId(),
							CODEXServerMessageType.SIGNED_UPDATE_REJECT_RESPONSE,
							SerializationUtil.serialize(swrr));

					sendMessage(csm, sm.getSenderId());
				}
				// SigShare SignedRes = stkm.sign(brr);

			} else if (sm.getType().equals(
					CODEXServerMessageType.SIGN_READ_ACCEPT_RESPONSE)) {

				SignReadResponseRequest srrr = (SignReadResponseRequest) SerializationUtil
						.deserialize(sm.getSerializedMessage());

				// Check the evidence set
				// To be done later

				BlindedReadResponse brr = srrr.getBrr();

				// At this time, the brr will be signed
				SignedReadResponse srr = new SignedReadResponse(
						stkm.sign(SerializationUtil.serialize(brr)));

				// println("Signed sig : " + srr.getSignedShare());
				// Now create the response to be sent back
				// The serialized message contains the brr
				CODEXServerMessage csm = new CODEXServerMessage(sm.getNonce(),
						getServerId(),
						CODEXServerMessageType.SIGNED_READ_ACCEPT_RESPONSE,
						SerializationUtil.serialize(srr));

				sendMessage(csm, sm.getSenderId());

				// SigShare SignedRes = stkm.sign(brr);

			} else if (sm.getType().equals(
					CODEXServerMessageType.READ_ACCEPT_RESPONSE)
					|| sm.getType().equals(
							CODEXServerMessageType.SIGNED_READ_ACCEPT_RESPONSE)

					|| sm.getType().equals(
							CODEXServerMessageType.TIMESTAMP_RESPONSE)
					|| sm.getType()
							.equals(CODEXServerMessageType.SIGNED_TIMESTAMP_ACCEPT_RESPONSE)

					|| sm.getType().equals(
							CODEXServerMessageType.UPDATE_ACCEPT_RESPONSE)
					|| sm.getType().equals(
							CODEXServerMessageType.UPDATE_REJECT_RESPONSE)
					|| sm.getType()
							.equals(CODEXServerMessageType.SIGNED_UPDATE_ACCEPT_RESPONSE)
					|| sm.getType()
							.equals(CODEXServerMessageType.SIGNED_UPDATE_REJECT_RESPONSE)

			) {

				println("Added a CODEX_SERVER_MESSAGE:" + sm.getType()
						+ " in message queue with nonce " + sm.getNonce());
				serverMessages.add(sm);

			} else if (sm.getType().equals(
					CODEXServerMessageType.UPDATE_REQUEST)) {

				UpdateRequest ur = (UpdateRequest) SerializationUtil
						.deserialize(sm.getSerializedMessage());
				boolean checkFailed = false;

				if (ur.getCcm() == null) {
					checkFailed = true;
				}

				if (!skm.verifyClientSignature(ur.getCcm())) {
					checkFailed = true;
				}

				// Add the client message to the clientMessages queue
				// Become the delegate for this client Message if not already
				// processed

				if (sm.getSenderId() != getServerId())
					addClientMessage(ur.getCcm());

				ClientUpdateRequest cwr = (ClientUpdateRequest) SerializationUtil
						.deserialize(ur.getCcm().getSerializedMessage());
				CODEXClientMessage ccm = cwr.getCcm();
				if (ccm == null
						|| !ccm.getType().equals(
								CODEXClientMessageType.TIMESTAMP_RESPONSE)
						|| !stkm.verifySignature(ccm)) {
					checkFailed = true;
				}

				if (checkFailed) {
					// Then check failed
					// Ignore the client request
					// Add server to compromised list
					println("CHECK for TIMESTAMP_RESPONSE FAILED");
					return;
				}

				TimeStampReadResponse trr = (TimeStampReadResponse) SerializationUtil
						.deserialize(ccm.getSerializedMessage());
				BigInteger correctTimeStamp = trr.getTimestamp().add(
						BigInteger.ONE);

				// Verify the evidence Set
				// Do this later

//				BigInteger dbTS = getTimestampFromDB(cwr.getDataId());
				BigInteger dbTS = stateManager.getTimestampFromDB(cwr.getDataId());
				if (dbTS.compareTo(correctTimeStamp) == -1) {
					// Local TS < Update TS
					// Update the share and send back an update_accept message
					// ?? Locally bind E(s) to name N ??
					println("Updating dataId " + cwr.getDataId()
							+ " with timestamp " + correctTimeStamp
							+ " and secret " + cwr.getEncryptedSecret());
//					shareDB.put(cwr.getDataId(), new SecretShare(
//							correctTimeStamp, cwr.getEncryptedSecret()));
					stateManager.update(cwr.getDataId(), new SecretShare(correctTimeStamp, cwr.getEncryptedSecret()));

					UpdateAcceptResponse uar = new UpdateAcceptResponse(
							ur.getCcm(), sm.getSenderId());

					CODEXServerMessage csm = new CODEXServerMessage(
							sm.getNonce(), getServerId(),
							CODEXServerMessageType.UPDATE_ACCEPT_RESPONSE,
							SerializationUtil.serialize(uar));

					sendMessage(csm, sm.getSenderId());

				} else {
					// Local TS >= Update TS
					// Send back a reject message to the original server
					// This is required for the original server to create
					// evidence set for reject message
					// to be sent back to client

					UpdateRejectResponse urr = new UpdateRejectResponse(
							ur.getCcm(), sm.getSenderId());

					CODEXServerMessage csm = new CODEXServerMessage(
							sm.getNonce(), getServerId(),
							CODEXServerMessageType.UPDATE_REJECT_RESPONSE,
							SerializationUtil.serialize(urr));

					sendMessage(csm, sm.getSenderId());

				}

			}
		}

		private void addClientMessage(CODEXClientMessage ccm) {

			if (!clientMessagesProcessed.contains(ccm.getNonce())) {
				if (!clientMessages.contains(ccm)) {
					clientMessages.offer(ccm);
					clientMessagesProcessed.add(ccm.getNonce());
				} else {
					println("Client message already added to queue. Nonce : "
							+ ccm.getNonce());
				}
			} else {
				println("Client message already processed. Nonce : "
						+ ccm.getNonce());
			}
		}
	}

	private class PRSSocketListener implements Runnable {

		@Override
		public void run() {
			// TODO Auto-generated method stub
			println("Running PRS server thread for server " + getServerId());
			try {
				prsConnectionSocket = new DatagramSocket(prsPort
						+ getServerId());
				// address = InetAddress.getByName(ReplicaServer.broadcastIP);
				// socket.joinGroup(address);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			printState();

			byte[] buf = new byte[16192];
			DatagramPacket packet = new DatagramPacket(buf, buf.length);

			while (true) {

				try {
					println("Waiting for server messages on socket : "
							+ prsConnectionSocket.getLocalPort());
					prsConnectionSocket.receive(packet);

					handlePRSPacket(packet);

					// printState();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

		}

		private void handlePRSPacket(DatagramPacket receivePacket) {
			PRSMessage sm = (PRSMessage) SerializationUtil
					.deserialize(receivePacket.getData());

			println("Received " + sm.getType() + " from PRS " + " with nonce "
					+ sm.getNonce());

			// Verify the server signature and return if not valid
			if (!skm.verifyPRSSignature(sm)) {
				System.out.println("Cannot verify signature from PRS message");
				return;
			}

			if (sm.getType().equals(PRSMessageType.DUMP_STATE)) {
				println("Setting dump variable");
				sm.setAddress(receivePacket.getAddress());
				sm.setPort(receivePacket.getPort());
				dumpMessage = sm;
				stopAcceptingNewClientMessages = true;

			}
		}
	}

	public void printState() {
		println("Current state of server " + getServerId() + " :");
		// for (Long s : messageCache.keySet()) {
		// System.out.println(s + " --  ");
		// for (CODEXServerMessage i : messageCache.get(s)) {
		// System.out.print(i + " ");
		// }
		// System.out.println();
		// }

//		for (String s : shareDB.keySet()) {
//			System.out.println(s + " : " + shareDB.get(s));
//		}
		this.stateManager.printState();
	}

	public int getServerId() {
		return serverId;
	}

	public void setServerId(int serverId) {
		this.serverId = serverId;
	}

	public void addSecret(String id, SecretShare ss) {
		this.stateManager.update(id, ss);
		printState();
	}

	public boolean isNonceEqual(CODEXServerMessage csm1, CODEXServerMessage csm2) {
		if (csm1 == null)
			return false;
		if (csm2 == null)
			return false;
		return csm1.getNonce() == csm2.getNonce();
	}

	
}
