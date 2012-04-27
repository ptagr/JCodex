package server;

import java.io.IOException;
import java.math.BigInteger;
import java.net.BindException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import client.messages.CODEXClientMessage;
import client.messages.CODEXClientMessageType;
import client.messages.ClientReadRequest;
import client.messages.ClientWriteRequest;

import main.AccessOrderCache;
import main.ConnectionInfo;
import server.messages.BlindedReadResponse;
import server.messages.CODEXServerMessage;
import server.messages.CODEXServerMessageType;
import server.messages.ForwardReadRequestAccept;
import server.messages.ForwardWriteRequestAccept;
import server.messages.SignReadResponseRequest;
import server.messages.SignWriteResponseRequest;
import server.messages.SignedReadResponse;
import server.messages.SignedWriteResponse;
import server.messages.VerifiedWriteRequest;
import server.messages.VerifyWriteRequest;
import server.messages.WriteSecretResponse;
import threshsig.SigShare;
import utils.SerializationUtil;
import utils.TimeUtility;

public class CopyOfServer implements Runnable {
	private int l;
	private int k;
	private int t;

	// public static String broadcastIP = "230.0.0.1";

	public static int baseServerPort = 7000;
	public static int timeOutForAcceptanceMessages = 10000; // time in mSec

	private int serverId;

	private ServerKeyManager skm;

	private ServerThresholdKeyManager stkm;

	// The thread listening to incoming messages on the broadcast channel
	private Thread bclThread;

	// The db to store the shares -> in memory as of now
	private final HashMap<String, SecretShare> shareDB;

	private Map<Integer, ConnectionInfo> serverConnectionInfo;

	private DatagramSocket clientConnectionSocket;

	private DatagramSocket serverConnectionSocket;

	ReentrantLock messageCacheLock = new ReentrantLock();

	private LinkedHashMap<Long, Set<CODEXServerMessage>> messageCache = new AccessOrderCache<Long, Set<CODEXServerMessage>>(
			50);

	// private LinkedHashMap<Long, Object> serverCheckCache = new
	// AccessOrderCache<Long, Object>(
	// 50);

	LinkedBlockingQueue<CODEXServerMessage> serverMessages = new LinkedBlockingQueue<CODEXServerMessage>(
			10);

	public CopyOfServer(int k, int l, int clientPort, int serverId,
			Set<Integer> clientIds) {
		this.k = k;
		this.t = k - 1;
		this.l = l;
		this.serverId = serverId;
		this.shareDB = new HashMap<String, SecretShare>();

		this.skm = new ServerKeyManager(clientIds, serverId, l);

		this.stkm = new ServerThresholdKeyManager(serverId);

		// INitialize and start the listener
		this.bclThread = new Thread(new ServerSocketListener());
		bclThread.start();

		try {
			this.clientConnectionSocket = new DatagramSocket(clientPort
					+ serverId);
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

	// private void checkSignatureOnMessage(CODEXMessage cm) {
	//
	// }

	@Override
	public void run() {
		// TODO Auto-generated method stub
		byte[] receiveData = new byte[8192];
		while (true) {
			DatagramPacket receivePacket = new DatagramPacket(receiveData,
					receiveData.length);
			try {
				println("Waiting for client messages on socket : "
						+ clientConnectionSocket.getLocalPort());
				this.clientConnectionSocket.receive(receivePacket);
				handlePacket(receivePacket);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

	// private void sendandReturn(CODEXServerMessage csm, ){
	//
	// }

	private void handlePacket(DatagramPacket receivePacket) {

		// System.out.println("Received a client request");
		CODEXClientMessage cm = (CODEXClientMessage) SerializationUtil
				.deserialize(receivePacket.getData());

		println("Received " + cm.getType() + " from client " + cm.getSenderId()
				+ " with nonce " + cm.getNonce());

		// Verify the client signature and return if not valid
		if (!skm.verifyClientSignature(cm)) {
			println("Cannot verify signature");
			return;
		}

		if (cm.getType().equals(
				CODEXClientMessageType.CLIENT_READ_SECRET_REQUEST)) {
			ClientReadRequest crr = (ClientReadRequest) SerializationUtil
					.deserialize(cm.getSerializedMessage());
			// println(crr.getEncryptedBlindingFactor().toString());

			CODEXServerMessage csm = new CODEXServerMessage(
					new Random().nextLong(), getServerId(),
					CODEXServerMessageType.FORWARD_READ_REQUEST,
					SerializationUtil.serialize(cm));

			println("Forwarding CLIENT_READ_SECRET_REQUEST message to "
					+ (3 * t + 1) + " servers");

			// Make an entry in serverCheckCache of <nonce, MR(n)>
			// serverCheckCache.put(csm.getNonce(), cm);

			// Send messages to 3t+1 servers selected randomly
			sendMessageToQuorum(csm);

			// Wait for atleast 2t+1 servers to respond to the request
			// while (messageCache.get(csm.getNonce()) == null
			// || messageCache.get(csm.getNonce()).size() < (2 * t + 1)) {
			// // Wait
			// }
			// try {
			// Thread.sleep(1000);
			// } catch (InterruptedException e) {
			// // TODO Auto-generated catch block
			// e.printStackTrace();
			// }

			boolean foundEvidenceSet = false;
			// Set<CODEXServerMessage> csmSet =
			// messageCache.get(csm.getNonce());
			HashMap<BigInteger, Set<ForwardReadRequestAccept>> evidenceMap = new HashMap<BigInteger, Set<ForwardReadRequestAccept>>();
			Set<ForwardReadRequestAccept> evidenceSet = null;
			BigInteger correctCipher = null;
			while (!foundEvidenceSet) {
				CODEXServerMessage csmtemp;
				try {
					csmtemp = serverMessages.poll(TimeUtility.timeOut,
							TimeUnit.MILLISECONDS);
					if (csmtemp == null) {
						println("No message received. Timing out");
						break;
					}
					if (csmtemp.getType().equals(
							CODEXServerMessageType.FORWARD_READ_REQUEST_ACCEPT)) {
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
								Set<ForwardReadRequestAccept> tempevidenceSet = evidenceMap
										.get(frra.getCipher());
								if (tempevidenceSet == null) {
									tempevidenceSet = new HashSet<ForwardReadRequestAccept>();
									evidenceMap.put(frra.getCipher(),
											tempevidenceSet);
								}

								tempevidenceSet.add(frra);
								if (tempevidenceSet.size() == (2 * t + 1)) {
									foundEvidenceSet = true;
									evidenceSet = tempevidenceSet;
									correctCipher = frra.getCipher();
								}

							}
						}
					}
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}

			// if (csmSet == null) {
			// // Handle this later
			// } else {
			//
			// //HashMap<BigInteger, Set<ForwardReadRequestAccept>> evidenceMap
			// = new HashMap<BigInteger, Set<ForwardReadRequestAccept>>();
			//
			// for (CODEXServerMessage csmtemp : csmSet) {
			// if (csmtemp.getType().equals(
			// CODEXServerMessageType.FORWARD_READ_REQUEST_ACCEPT)) {
			// ForwardReadRequestAccept frra = (ForwardReadRequestAccept)
			// SerializationUtil
			// .deserialize(csmtemp.getSerializedMessage());
			//
			// // Now check the signature of MR(n)
			// if (skm.verifyServerSignature(SerializationUtil
			// .serialize(cm), frra.getDigitalSig()
			// .toByteArray(), csmtemp.getSenderId())) {
			//
			// // Now check the validity of the decrypted partial
			// // share
			// if (stkm.verifyDecryptedShare(frra.getCipher()
			// .toByteArray(), frra.getDecryptedShare())) {
			// Set<ForwardReadRequestAccept> evidenceSet = evidenceMap
			// .get(frra.getCipher());
			// if (evidenceSet == null) {
			// evidenceSet = new HashSet<ForwardReadRequestAccept>();
			// evidenceMap.put(frra.getCipher(),
			// evidenceSet);
			// }
			//
			// evidenceSet.add(frra);
			//
			// }
			// }
			// }
			// }
			//
			// Set<ForwardReadRequestAccept> evidenceSet = null;
			// BigInteger correctCipher = null;
			// // BigInteger digitalSig = null;
			// // Now find the evidence set with at least t+1 valid responses
			// // with same ciphers
			// for (BigInteger bi : evidenceMap.keySet()) {
			// if (evidenceMap.get(bi).size() >= (t + 1)) {
			// evidenceSet = evidenceMap.get(bi);
			// correctCipher = bi;
			//
			// break;
			// }
			//
			// }

			if (evidenceSet == null) {
				// Handle this later
				System.out.println("Evidence set null");
			} else {
				// REceived t+1 pieces of evidence

				// Get t+1 sigshares to decrypt cipher
				Set<SigShare> shares = new HashSet<SigShare>();
				int i = 0;
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
						CODEXServerMessageType.SIGN_READ_RESPONSE_REQUEST,
						SerializationUtil.serialize(srrr));

				// Invoke a threshold signature protocol with 3t+1 servers
				// to sign the response message
				sendMessageToQuorum(csm2);

				// // Wait for atleast 2t+1 servers to respond to the request
				// while (messageCache.get(csm2.getNonce()) == null
				// || messageCache.get(csm2.getNonce()).size() < (2 * t + 1)) {
				// // Wait
				// }
				//
				// try {
				// Thread.sleep(1000);
				// } catch (InterruptedException e) {
				// // TODO Auto-generated catch block
				// e.printStackTrace();
				// }

				boolean signatureGenerated = false;
				BigInteger digitalSigRes = null;
				Set<SigShare> sigs = new HashSet<SigShare>();
				byte[] brrBytes = SerializationUtil.serialize(brr);
				while (!signatureGenerated) {
					CODEXServerMessage csmtemp;
					try {
						csmtemp = serverMessages.poll(TimeUtility.timeOut,
								TimeUnit.MILLISECONDS);

						if (csmtemp == null) {
							println("No message received. Timing out");
							break;
						}

						if (csmtemp.getType().equals(
								CODEXServerMessageType.SIGNED_READ_RESPONSE)) {
							SignedReadResponse srr = (SignedReadResponse) SerializationUtil
									.deserialize(csmtemp.getSerializedMessage());

							// Verify the signature on blinded response
							if (stkm.verifySignedShare(brrBytes,
									srr.getSignedShare())) {
								sigs.add(srr.getSignedShare());
								if (sigs.size() == (t + 1)) {
									println("Got t+1 signatures : "+sigs.size());
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
										signatureGenerated = true;
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

				CODEXClientMessage clientRes = new CODEXClientMessage(
						cm.getNonce(), getServerId(),
						CODEXClientMessageType.BLINDED_READ_RESPONSE, brrBytes);
				clientRes.setSerializedMessageSignature(digitalSigRes
						.toByteArray());

				// Send back response
				println("Send back client reponse on host "
						+ receivePacket.getAddress() + " and port "
						+ receivePacket.getPort());
				sendResponseMessage(clientRes, receivePacket.getAddress(),
						receivePacket.getPort());

			}

		} else if (cm.getType().equals(
				CODEXClientMessageType.CLIENT_WRITE_SECRET_REQUEST)) {

			// Write Protocol starts here
			ClientWriteRequest crw = (ClientWriteRequest) SerializationUtil
					.deserialize(cm.getSerializedMessage());
			// println(crr.getEncryptedBlindingFactor().toString());

			CODEXServerMessage csm = new CODEXServerMessage(
					new Random().nextLong(), getServerId(),
					CODEXServerMessageType.FORWARD_WRITE_REQUEST,
					SerializationUtil.serialize(cm));

			println("Forwarding " + cm.getType() + " message to " + (3 * t + 1)
					+ " servers");
			// Send messages to 3t+1 servers selected randomly
			sendMessageToQuorum(csm);

			// Wait for atleast 3t+1 servers to respond to the request
			// or a timeout to occur
			TimeUtility tu = new TimeUtility();
			while ((messageCache.get(csm.getNonce()) == null || messageCache
					.get(csm.getNonce()).size() < (3 * t + 1))
					&& tu.timerHasNotExpired()) {
				// Wait
			}
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			if (messageCache.get(csm.getNonce()) == null) {
				// Handle this later
			} else {

				Set<ForwardWriteRequestAccept> evidenceSet = new HashSet<ForwardWriteRequestAccept>();

				//
				Set<CODEXServerMessage> csmSet = messageCache.get(csm
						.getNonce());
				for (CODEXServerMessage csmtemp : csmSet) {
					if (csmtemp
							.getType()
							.equals(CODEXServerMessageType.FORWARD_WRITE_REQUEST_ACCEPT)) {
						ForwardWriteRequestAccept frrw = (ForwardWriteRequestAccept) SerializationUtil
								.deserialize(csmtemp.getSerializedMessage());

						// Now check the signature of MW(n)
						if (skm.verifyServerSignature(SerializationUtil
								.serialize(cm), frrw.getDigitalSig()
								.toByteArray(), csmtemp.getSenderId())) {

							evidenceSet.add(frrw);

						}
					}
				}

				if (evidenceSet.size() < (2 * t + 1)) {
					// Received less than 2t+1 pieces of evidence
					// Handle this later
				} else {
					// Received 2t+1 pieces of evidence

					// Create the write reponse to be sent to client
					WriteSecretResponse wsr = new WriteSecretResponse(crw,
							crw.getDataId());

					// Create the VERIFY message to be sent to other servers
					VerifyWriteRequest vrw = new VerifyWriteRequest(crw, wsr,
							evidenceSet);

					CODEXServerMessage csm_verify = new CODEXServerMessage(
							new Random().nextLong(), getServerId(),
							CODEXServerMessageType.VERIFY_WRITE_REQUEST,
							SerializationUtil.serialize(vrw));

					println("Sending " + csm_verify.getType() + " message to "
							+ (3 * t + 1) + " servers");

					// Send messages to 3t+1 servers selected randomly
					sendMessageToQuorum(csm_verify);

					// Wait for atleast 3t+1 servers to respond to the request
					// or a timeout to occur
					tu.reset();
					while ((messageCache.get(csm_verify.getNonce()) == null || messageCache
							.get(csm_verify.getNonce()).size() < (3 * t + 1))
							&& tu.timerHasNotExpired()) {
						// Wait
					}
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

					Set<VerifiedWriteRequest> verifiedEvidenceSet = new HashSet<VerifiedWriteRequest>();

					//
					csmSet = messageCache.get(csm_verify.getNonce());
					for (CODEXServerMessage csmtemp : csmSet) {
						if (csmtemp.getType().equals(
								CODEXServerMessageType.VERIFIED_WRITE_REQUEST)) {
							VerifiedWriteRequest vdwr = (VerifiedWriteRequest) SerializationUtil
									.deserialize(csmtemp.getSerializedMessage());

							// Now check the signature of MW(n)
							if (skm.verifyServerSignature(SerializationUtil
									.serialize(wsr), vdwr.getDigitalSig()
									.toByteArray(), csmtemp.getSenderId())) {

								verifiedEvidenceSet.add(vdwr);

							}
						}
					}

					if (verifiedEvidenceSet.size() < (2 * t + 1)) {
						// Received less than 2t+1 pieces of evidence
						// Handle this later
					} else {

						// Create the sign request
						SignWriteResponseRequest swrr = new SignWriteResponseRequest(
								verifiedEvidenceSet, wsr);

						CODEXServerMessage csm_swrr = new CODEXServerMessage(
								new Random().nextLong(),
								getServerId(),
								CODEXServerMessageType.SIGN_WRITE_RESPONSE_REQUEST,
								SerializationUtil.serialize(swrr));

						// Invoke a threshold signature protocol with 3t+1
						// servers
						// to sign the response message
						sendMessageToQuorum(csm_swrr);

						// Wait for atleast 2t+1 servers to respond to the
						// request
						tu.reset();
						while ((messageCache.get(csm_swrr.getNonce()) == null || messageCache
								.get(csm_swrr.getNonce()).size() < (2 * t + 1))
								&& tu.timerHasNotExpired()) {
							// Wait
						}

						try {
							Thread.sleep(1000);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

						Set<SigShare> sigs = new HashSet<SigShare>();

						byte[] wsrBytes = SerializationUtil.serialize(wsr);

						if (messageCache.get(csm_swrr.getNonce()) == null) {
							// Handle this later
							// Someone deleted the messages
							// Panic
						} else {
							Set<CODEXServerMessage> csmSet2 = messageCache
									.get(csm_swrr.getNonce());
							// sigs.clear();
							for (CODEXServerMessage csmtemp : csmSet2) {
								if (csmtemp
										.getType()
										.equals(CODEXServerMessageType.SIGNED_WRITE_RESPONSE)) {
									SignedWriteResponse swr = (SignedWriteResponse) SerializationUtil
											.deserialize(csmtemp
													.getSerializedMessage());

									// Verify the signature on write response
									if (stkm.verifySignedShare(wsrBytes,
											swr.getSignedShare())) {
										sigs.add(swr.getSignedShare());
									} else {
										println("Cannot verify Write Response");
									}
								}
							}

							// Use the partial signatures to create the signed
							// response for the client
							BigInteger digitalSigRes = stkm.thresholdSign(
									wsrBytes, sigs);

							// println("Digital Signature on response message : "+digitalSigRes);

							// Verify the digital signature just in case
							if (!stkm.verifySignature(wsrBytes, digitalSigRes)) {
								// Panic mode
								// The threshold generated signature on the
								// response
								// message is incorrect
							}

							CODEXClientMessage clientRes = new CODEXClientMessage(
									cm.getNonce(),
									getServerId(),
									CODEXClientMessageType.WRITE_SECRET_REPONSE,
									wsrBytes);
							clientRes
									.setSerializedMessageSignature(digitalSigRes
											.toByteArray());

							// Send back response
							println("Send back client reponse on host "
									+ receivePacket.getAddress() + " and port "
									+ receivePacket.getPort());
							sendResponseMessage(clientRes,
									receivePacket.getAddress(),
									receivePacket.getPort());

						}
					}

				}
			}

		}

	}

	private void println(String string) {
		System.out.println("Server " + this.getServerId() + " : " + string);

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

	public boolean isMessageCount(String key, int count) {
		if (messageCache.containsKey(key)) {
			if (messageCache.get(key).size() >= count)
				return true;
		}
		return false;
	}

	public Set<Integer> sendMessageToQuorum(CODEXServerMessage csm) {
		// Clear the server messages queue
		serverMessages.clear();

		if (csm.getSerializedMessage() == null) {
			return null;
		}

		Random r = new Random();
		Set<Integer> serversToSend = new HashSet<Integer>();
		while (serversToSend.size() != 3 * t + 1) {
			serversToSend.add(r.nextInt(l));
		}

		for (Integer in : serversToSend) {
			sendMessage(csm, in);
		}
		return serversToSend;
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

		InetAddress address;

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

			byte[] buf = new byte[16192];
			DatagramPacket packet = new DatagramPacket(buf, buf.length);

			while (true) {

				try {
					println("Waiting for server messages on socket : "
							+ serverConnectionSocket.getLocalPort());
					serverConnectionSocket.receive(packet);

					handleServerPacket(packet);
					// AcceptancePacket aPacket = AcceptancePacket
					// .deserialize(packet.getData());
					// Set<Integer> count = null;
					// if (messageCache.containsKey(aPacket.toString())) {
					// count = messageCache.get(aPacket.toString());
					// } else {
					// count = new HashSet<Integer>();
					// }
					// count.add((int) aPacket.getSenderId());
					// messageCache.put(aPacket.toString(), count);

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

			if (sm.getType()
					.equals(CODEXServerMessageType.FORWARD_READ_REQUEST)) {
				CODEXClientMessage ccm = (CODEXClientMessage) SerializationUtil
						.deserialize(sm.getSerializedMessage());

				// Verify client message
				if (!skm.verifyClientSignature(ccm)) {
					// Send a FORWARD_READ_REQUEST_REJECT
					CODEXServerMessage csm = new CODEXServerMessage(
							sm.getNonce(), getServerId(),
							CODEXServerMessageType.FORWARD_READ_REQUEST_REJECT,
							SerializationUtil.serialize(ccm));

					sendMessage(csm, sm.getSenderId());
				} else {
					// Client Message is correct
					ClientReadRequest crr = (ClientReadRequest) SerializationUtil
							.deserialize(ccm.getSerializedMessage());

					// Check if some value val(n) is locally bound to N
					SecretShare ss = shareDB.get(crr.getDataId());

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

					// Create the FORWARD_READ_REQUEST_ACCEPT message to send
					// back
					ForwardReadRequestAccept frra = new ForwardReadRequestAccept(
							new BigInteger(skm.getSignature(ccm)), cipher,
							decryptedShare, ss.getTimestamp());

					CODEXServerMessage csm = new CODEXServerMessage(
							sm.getNonce(), getServerId(),
							CODEXServerMessageType.FORWARD_READ_REQUEST_ACCEPT,
							SerializationUtil.serialize(frra));

					sendMessage(csm, sm.getSenderId());
				}

			} else if (sm.getType().equals(
					CODEXServerMessageType.SIGN_READ_RESPONSE_REQUEST)) {

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
						CODEXServerMessageType.SIGNED_READ_RESPONSE,
						SerializationUtil.serialize(srr));

				sendMessage(csm, sm.getSenderId());

				// SigShare SignedRes = stkm.sign(brr);

			} else if (sm.getType().equals(
					CODEXServerMessageType.FORWARD_READ_REQUEST_ACCEPT)
					|| sm.getType()
							.equals(CODEXServerMessageType.FORWARD_WRITE_REQUEST_ACCEPT)
					|| sm.getType().equals(
							CODEXServerMessageType.SIGNED_WRITE_RESPONSE)
					|| sm.getType().equals(
							CODEXServerMessageType.SIGNED_READ_RESPONSE)
					|| sm.getType().equals(
							CODEXServerMessageType.VERIFIED_WRITE_REQUEST)) {

				println("Added a CODEX_SERVER_MESSAGE:" + sm.getType()
						+ " in message queue with nonce " + sm.getNonce());
				serverMessages.add(sm);

				// Object objectToCheck = serverCheckCache.get(sm.getNonce());
				// if (objectToCheck == null) {
				// println("No object to check the message against. Ignore message");
				// } else {
				//
				// ForwardReadRequestAccept frra = (ForwardReadRequestAccept)
				// SerializationUtil
				// .deserialize(sm.getSerializedMessage());
				//
				// // Now check the signature of MR(n)
				// if (skm.verifyServerSignature(SerializationUtil
				// .serialize(objectToCheck), frra.getDigitalSig()
				// .toByteArray(), sm.getSenderId())) {
				//
				// // Now check the validity of the decrypted partial
				// // share
				// if (stkm.verifyDecryptedShare(frra.getCipher()
				// .toByteArray(), frra.getDecryptedShare())) {
				//
				// // The message is verified and is valid
				// println("Verified and Added a CODEX_SERVER_MESSAGE:"
				// + sm.getType()
				// + " in message queue with nonce "
				// + sm.getNonce());
				// // Add the message to the message cache
				// serverMessages.add(sm);
				//
				// // Set<CODEXServerMessage> messageSet = messageCache
				// // .get(sm.getNonce());
				// // if (messageSet != null
				// // && messageSet.size() == (2 * t + 1)) {
				// // // Already received a quorum of correct
				// // // messages, so ignore this message
				// // println("Ignoring the message");
				// // } else {
				// //
				// // if (messageSet == null) {
				// // // messageSet =
				// // // Collections.synchronizedSet(new
				// // // HashSet<CODEXServerMessage>());
				// // messageSet = new HashSet<CODEXServerMessage>();
				// // messageCache.put(sm.getNonce(), messageSet);
				// // }
				// // messageSet.add(sm);
				// // println("Message Set size : "
				// // + messageSet.size());
				// // }
				// }
				// }
				// }
				//
				// } else if (sm.getType().equals(
				// CODEXServerMessageType.FORWARD_WRITE_REQUEST_ACCEPT)
				// || sm.getType().equals(
				// CODEXServerMessageType.SIGNED_WRITE_RESPONSE)
				// || sm.getType().equals(
				// CODEXServerMessageType.SIGNED_READ_RESPONSE)
				// || sm.getType().equals(
				// CODEXServerMessageType.VERIFIED_WRITE_REQUEST)) {
				//
				// println("Added a CODEX_SERVER_MESSAGE:" + sm.getType()
				// + " in message queue with nonce " + sm.getNonce());
				// // Add the message to the message cache
				// Set<CODEXServerMessage> messageSet = messageCache.get(sm
				// .getNonce());
				// if (messageSet == null) {
				// // messageSet = Collections.synchronizedSet(new
				// // HashSet<CODEXServerMessage>());
				// messageSet = new HashSet<CODEXServerMessage>();
				// messageCache.put(sm.getNonce(), messageSet);
				// }
				// messageSet.add(sm);
				// println("Message Set size : " + messageSet.size());

			} else if (sm.getType().equals(
					CODEXServerMessageType.FORWARD_WRITE_REQUEST)) {
				CODEXClientMessage ccm = (CODEXClientMessage) SerializationUtil
						.deserialize(sm.getSerializedMessage());

				// Verify client message
				if (!skm.verifyClientSignature(ccm)) {
					// Send a FORWARD_WRITE_REQUEST_REJECT
					CODEXServerMessage csm = new CODEXServerMessage(
							sm.getNonce(),
							getServerId(),
							CODEXServerMessageType.FORWARD_WRITE_REQUEST_REJECT,
							SerializationUtil.serialize(ccm));

					sendMessage(csm, sm.getSenderId());
				} else {
					// Client Message is correct
					ClientWriteRequest crw = (ClientWriteRequest) SerializationUtil
							.deserialize(ccm.getSerializedMessage());

					// ?? Locally bind E(s) to name N ??

					// Create the FORWARD_WRITE_REQUEST_ACCEPT message to send
					// back
					ForwardWriteRequestAccept frrw = new ForwardWriteRequestAccept(
							new BigInteger(skm.getSignature(ccm)));

					CODEXServerMessage csm = new CODEXServerMessage(
							sm.getNonce(),
							getServerId(),
							CODEXServerMessageType.FORWARD_WRITE_REQUEST_ACCEPT,
							SerializationUtil.serialize(frrw));

					sendMessage(csm, sm.getSenderId());
				}

			} else if (sm.getType().equals(
					CODEXServerMessageType.VERIFY_WRITE_REQUEST)) {

				VerifyWriteRequest vwr = (VerifyWriteRequest) SerializationUtil
						.deserialize(sm.getSerializedMessage());

				// the signature of sender Server is already verified above

				// Verify the evidence Set Ed
				// To do later
				// Just do basic check to see if size is at least 2t+1
				if (vwr.getEvidenceSet().size() < (2 * t + 1)) {
					// Evidence set check fail
					// Send REJECT message
					// later

				} else {
					// Evidence set check passes

					// Bind E(s) from MW(n) to name N
					ClientWriteRequest cwr = vwr.getCwr();

					// ?? Verify signature of MW(n) here ??
					BigInteger ts = null;
					if (shareDB.containsKey(cwr.getDataId())) {
						ts = shareDB.get(cwr.getDataId()).getTimestamp();
					} else {
						ts = BigInteger.ZERO;
					}

					// Update db here
					shareDB.put(
							cwr.getDataId(),
							new SecretShare(ts.add(BigInteger.ONE), cwr
									.getEncryptedSecret()));

					// Send VERIFIED message back to sender Server
					VerifiedWriteRequest vdwr = new VerifiedWriteRequest(
							new BigInteger(skm.getSignature(vwr.getWsr())));

					CODEXServerMessage csm = new CODEXServerMessage(
							sm.getNonce(), getServerId(),
							CODEXServerMessageType.VERIFIED_WRITE_REQUEST,
							SerializationUtil.serialize(vdwr));

					sendMessage(csm, sm.getSenderId());

				}

			} else if (sm.getType().equals(
					CODEXServerMessageType.SIGN_WRITE_RESPONSE_REQUEST)) {

				SignWriteResponseRequest swrr = (SignWriteResponseRequest) SerializationUtil
						.deserialize(sm.getSerializedMessage());

				// Check the evidence set
				// To be done later

				WriteSecretResponse wsr = swrr.getWsr();

				// At this time, the wsr will be signed
				SignedWriteResponse swr = new SignedWriteResponse(
						stkm.sign(SerializationUtil.serialize(wsr)));

				// println("Signed sig : " + srr.getSignedShare());
				// Now create the response to be sent back
				// The serialized message contains the brr
				CODEXServerMessage csm = new CODEXServerMessage(sm.getNonce(),
						getServerId(),
						CODEXServerMessageType.SIGNED_WRITE_RESPONSE,
						SerializationUtil.serialize(swr));

				sendMessage(csm, sm.getSenderId());

				// SigShare SignedRes = stkm.sign(brr);

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

		for (String s : shareDB.keySet()) {
			System.out.println(s + " : " + shareDB.get(s));
		}
	}

	public int getServerId() {
		return serverId;
	}

	public void setServerId(int serverId) {
		this.serverId = serverId;
	}

	public void addSecret(String id, SecretShare ss) {
		this.shareDB.put(id, ss);
		printState();
	}

}
