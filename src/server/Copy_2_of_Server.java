//package server;
//
//import java.io.IOException;
//import java.math.BigInteger;
//import java.net.BindException;
//import java.net.DatagramPacket;
//import java.net.DatagramSocket;
//import java.net.InetAddress;
//import java.net.SocketException;
//import java.util.HashMap;
//import java.util.HashSet;
//import java.util.Map;
//import java.util.Random;
//import java.util.Set;
//import java.util.concurrent.LinkedBlockingQueue;
//import java.util.concurrent.TimeUnit;
//
//import main.ConnectionInfo;
//import main.Constants;
//import server.messages.BlindedReadResponse;
//import server.messages.CODEXServerMessage;
//import server.messages.CODEXServerMessageType;
//import server.messages.ForwardReadRequestAccept;
//import server.messages.ForwardWriteRequest;
//import server.messages.ForwardWriteRequestAccept;
//import server.messages.InternalServerMessage;
//import server.messages.SignReadResponseRequest;
//import server.messages.SignTimeStampResponseRequest;
//import server.messages.SignUpdateAcceptResponse;
//import server.messages.SignedReadResponse;
//import server.messages.SignedTimeStampResponse;
//import server.messages.SignedUpdateAcceptResponse;
//import server.messages.TimeStampReadResponse;
//import server.messages.TimeStampRequest;
//import server.messages.TimeStampResponse;
//import server.messages.VerifiedWriteRequest;
//import server.messages.VerifyWriteRequest;
//import server.messages.ClientUpdateAcceptResponse;
//import threshsig.SigShare;
//import utils.SerializationUtil;
//import utils.TimeUtility;
//import client.messages.CODEXClientMessage;
//import client.messages.CODEXClientMessageType;
//import client.messages.ClientReadRequest;
//import client.messages.ClientTimeStampRequest;
//import client.messages.ClientUpdateRequest;
//
//public class Copy_2_of_Server implements Runnable {
//	private int l;
//	private int k;
//	private int t;
//
//	int quorumSize;
//
//	// public static String broadcastIP = "230.0.0.1";
//
//	public static int baseServerPort = 7000;
//	public static int timeOutForAcceptanceMessages = 10000; // time in mSec
//
//	private int serverId;
//
//	private ServerKeyManager skm;
//
//	private ServerThresholdKeyManager stkm;
//
//	// private DateFormat dateFormatter = new SimpleDateFormat("ss:S");
//
//	private long timer = 0;
//
//	// The thread listening to incoming messages on the broadcast channel
//	private Thread bclThread;
//
//	// The db to store the shares -> in memory as of now
//	private final HashMap<String, SecretShare> shareDB;
//
//	private Map<Integer, ConnectionInfo> serverConnectionInfo;
//
//	private DatagramSocket clientConnectionSocket;
//
//	private DatagramSocket serverConnectionSocket;
//
//	// ReentrantLock messageCacheLock = new ReentrantLock();
//	//
//	// private LinkedHashMap<Long, Set<CODEXServerMessage>> messageCache = new
//	// AccessOrderCache<Long, Set<CODEXServerMessage>>(
//	// 50);
//
//	// private LinkedHashMap<Long, Object> serverCheckCache = new
//	// AccessOrderCache<Long, Object>(
//	// 50);
//
//	LinkedBlockingQueue<CODEXServerMessage> serverMessages = new LinkedBlockingQueue<CODEXServerMessage>(
//			10);
//
//	public Copy_2_of_Server(int k, int l, int clientPort, int serverId,
//			Set<Integer> clientIds) {
//		this.k = k;
//		this.t = k - 1;
//		this.l = l;
//
//		this.quorumSize = 2 * t + 1;
//
//		this.serverId = serverId;
//		this.shareDB = new HashMap<String, SecretShare>();
//
//		this.skm = new ServerKeyManager(clientIds, serverId, l);
//
//		this.stkm = new ServerThresholdKeyManager(serverId);
//
//		// INitialize and start the listener
//		this.bclThread = new Thread(new ServerSocketListener());
//		bclThread.start();
//
//		try {
//			this.clientConnectionSocket = new DatagramSocket(clientPort
//					+ serverId);
//			// this.serverSocket.setSoTimeout(TimeUtility.timeOut);
//		} catch (SocketException e) {
//
//			if (e instanceof BindException) {
//				println("Port already in use");
//			}
//
//			e.printStackTrace();
//		}
//
//		serverConnectionInfo = new HashMap<Integer, ConnectionInfo>();
//
//		// Add connections for all servers except one that matches the id
//		// connection)
//		for (int i = 0; i < l; i++) {
//			// if (i != serverId)
//			serverConnectionInfo.put(i, new ConnectionInfo("localhost",
//					baseServerPort + i));
//		}
//	}
//
//	@Override
//	public void run() {
//		// TODO Auto-generated method stub
//		byte[] receiveData = new byte[8192];
//		TimeUtility tu = new TimeUtility();
//		while (true) {
//			DatagramPacket receivePacket = new DatagramPacket(receiveData,
//					receiveData.length);
//			try {
//				println("Waiting for client messages on socket : "
//						+ clientConnectionSocket.getLocalPort());
//				this.clientConnectionSocket.receive(receivePacket);
//				tu.reset();
//				handlePacket(receivePacket);
//				System.out.println("Handled packet in " + tu.delta() + " ms");
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		}
//
//	}
//
//	private void handlePacket(DatagramPacket receivePacket) {
//
//		// System.out.println("Received a client request");
//		CODEXClientMessage cm = (CODEXClientMessage) SerializationUtil
//				.deserialize(receivePacket.getData());
//		TimeUtility tu = new TimeUtility();
//		println("Received " + cm.getType() + " from client " + cm.getSenderId()
//				+ " with nonce " + cm.getNonce());
//
//		// Verify the client signature and return if not valid
//		if (!skm.verifyClientSignature(cm)) {
//			println("Cannot verify signature");
//			return;
//		}
//		if (cm.getType().equals(
//				CODEXClientMessageType.CLIENT_TIMESTAMP_REQUEST)) {
//
//			ClientTimeStampRequest ctr = (ClientTimeStampRequest) SerializationUtil
//					.deserialize(cm.getSerializedMessage());
//
//			// Request a timestamp from a quorum of servers
//			TimeStampRequest tsr = new TimeStampRequest(cm);
//
//			// CODEXServerMessage csm_tsr = new CODEXServerMessage(
//			// new Random().nextLong(), getServerId(),
//			// CODEXServerMessageType.FORWARD_TIMESTAMP_REQUEST,
//			// SerializationUtil.serialize(tsr));
//
//			long nonce_ftr = sendMessageToAllServers(tsr,
//					CODEXServerMessageType.FORWARD_TIMESTAMP_REQUEST);
//
//			// Now wait for responses of type FORWARD_TIMESTAMP_RESPONSE from a
//			// quorum
//			// of servers
//			// Now wait for a quorum number of messages
//			HashMap<BigInteger, Set<CODEXServerMessage>> evidenceMap = new HashMap<BigInteger, Set<CODEXServerMessage>>();
////			/Set<CODEXServerMessage> rejectSet = new HashSet<CODEXServerMessage>();
//			tu.reset();
//			int messagesReceived = 0;
//			while (tu.timerHasNotExpired()) {
//				CODEXServerMessage csmtemp;
//				try {
//					if (messagesReceived == quorumSize)
//						break;
//					csmtemp = serverMessages.poll(TimeUtility.timeOut,
//							TimeUnit.MILLISECONDS);
//					if (csmtemp == null) {
//						println("No message received. Timing out");
//						break;
//					}
//					if (nonce_ftr == csmtemp.getNonce()) {
//						if (csmtemp
//								.getType()
//								.equals(CODEXServerMessageType.FORWARD_TIMESTAMP_RESPONSE)) {
//
//							TimeStampResponse tsres = (TimeStampResponse) SerializationUtil
//									.deserialize(csmtemp.getSerializedMessage());
//
//							// Check whether tsres.getCcm() == cm
//							// Later
//
//							Set<CODEXServerMessage> tempevidenceSet = evidenceMap
//									.get(tsres.getTimeStamp());
//							if (tempevidenceSet == null) {
//								tempevidenceSet = new HashSet<CODEXServerMessage>();
//								evidenceMap.put(tsres.getTimeStamp(),
//										tempevidenceSet);
//							}
//
//							tempevidenceSet.add(csmtemp);
//
//							messagesReceived++;
//
//						}
////						else if (csmtemp
////								.getType()
////								.equals(CODEXServerMessageType.FORWARD_TIMESTAMP_REQUEST_REJECT)) {
////
////							TimeStampResponse tsres = (TimeStampResponse) SerializationUtil
////									.deserialize(csmtemp.getSerializedMessage());
////
////							// Check whether tsres.getCcm() == cm
////							// Later
////
////							rejectSet.add(csmtemp);
////
////							if (rejectSet.size() == (t + 1)) {
////								// We have got t+1 rejections
////								// Should never get reach here since the only
////								// time a server
////								// will reject a TSREQ message is when the
////								// client message is incorrectly signed
////								// But it has been verfied before
////								break;
////
////							}
////						}
//					}
//
//				} catch (InterruptedException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//
//			}
//
////			if (rejectSet.size() == (t + 1)) {
////				// Handle this later
////				// Should never get reach here since the only time a server
////				// will reject a TSREQ message is when the client message is
////				// incorrectly signed
////				// But it has been verfied before
////			} 
////			else 
//			{
//
//				// There might be multiple sets with t+1 entries
//				// This might happen because the compromised servers might team
//				// up
//				// with servers with old state to send the old timestamp
//				// To break the tie, select the set with higher TimeStamp
//				Set<CODEXServerMessage> evidenceSetTS = null;
//				BigInteger correctTimeStamp = null;
//				for (BigInteger key : evidenceMap.keySet()) {
//					Set<CODEXServerMessage> tsSet = evidenceMap.get(key);
//					if (tsSet.size() >= t + 1) {
//						if (evidenceSetTS == null
//								|| (correctTimeStamp != null && key
//										.compareTo(correctTimeStamp) == 1)) {
//							evidenceSetTS = tsSet;
//							correctTimeStamp = key;
//						}
//					}
//				}
//
//				if (evidenceSetTS == null) {
//					// Handle this later
//					// No timestamp found
//					println("EvidenceSet for TimestampResponse is null");
//					println("I should be compromised");
//				}
//
//				println("MAJORITY : Got " + evidenceSetTS.size()
//						+ " messages with timestamp " + correctTimeStamp);
//				// REceived t+1 pieces of evidence
//
//				TimeStampReadResponse trr = new TimeStampReadResponse(cm,
//						correctTimeStamp, ctr.getDataId());
//
//				// Create the sign request
//				SignTimeStampResponseRequest strr = new SignTimeStampResponseRequest(
//						evidenceSetTS, trr);
//
//				// CODEXServerMessage csm2 = new CODEXServerMessage(
//				// new Random().nextLong(), getServerId(),
//				// CODEXServerMessageType.SIGN_TIMESTAMP_ACCEPT_RESPONSE,
//				// SerializationUtil.serialize(strr));
//
//				// Invoke a threshold signature protocol with all servers
//				// to sign the response message
//				// sendMessageToAllServers(csm2);
//				long nonce_strr = sendMessageToAllServers(strr,
//						CODEXServerMessageType.SIGN_TIMESTAMP_ACCEPT_RESPONSE);
//
//				BigInteger digitalSigRes = null;
//				Set<SigShare> sigs = new HashSet<SigShare>();
//				byte[] trrBytes = SerializationUtil.serialize(trr);
//				tu.reset();
//				while (tu.timerHasNotExpired()) {
//					CODEXServerMessage csmtemp;
//					try {
//						csmtemp = serverMessages.poll(TimeUtility.timeOut,
//								TimeUnit.MILLISECONDS);
//
//						if (csmtemp == null) {
//							println("No message received. Timing out");
//							break;
//						}
//						if (nonce_strr == csmtemp.getNonce()) {
//							if (csmtemp
//									.getType()
//									.equals(CODEXServerMessageType.SIGNED_TIMESTAMP_ACCEPT_RESPONSE)) {
//								SignedTimeStampResponse str = (SignedTimeStampResponse) SerializationUtil
//										.deserialize(csmtemp
//												.getSerializedMessage());
//
//								// Verify the signature on blinded response
//								if (stkm.verifySignedShare(trrBytes,
//										str.getSignedShare())) {
//									sigs.add(str.getSignedShare());
//									if (sigs.size() >= (t + 1)) {
//										println("Got t+1 signatures : "
//												+ sigs.size());
//										// Try generating the threshold
//										// signature
//										// Use the partial signatures to create
//										// the
//										// signed
//										// response for the client
//										digitalSigRes = stkm.thresholdSign(
//												trrBytes, sigs);
//										if (digitalSigRes == null) {
//											// Signature could not be generated
//											// So continue the loop and try one
//											// more
//											// signature
//										} else {
//											break;
//										}
//
//									}
//
//								} else {
//									println("Cannot verify timestamp response");
//								}
//
//							}
//						}
//					
//					} catch (InterruptedException e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					}
//
//				}
//
//				if (digitalSigRes == null) {
//					// Could not generate threshold signature
//					// Handle this later
//					error("Could not generate a valid threshold signature");
//				} else {
//
//					CODEXClientMessage clientRes = new CODEXClientMessage(
//							cm.getNonce(), getServerId(),
//							CODEXClientMessageType.TIMESTAMP_RESPONSE, trrBytes);
//					clientRes.setSerializedMessageSignature(digitalSigRes
//							.toByteArray());
//
//					// Send back response
//					println("Send back client reponse on host "
//							+ receivePacket.getAddress() + " and port "
//							+ receivePacket.getPort());
//					sendResponseMessage(clientRes, receivePacket.getAddress(),
//							receivePacket.getPort());
//				}
//			}
//
//		} else if (cm.getType().equals(
//				CODEXClientMessageType.CLIENT_READ_REQUEST)) {
//			ClientReadRequest crr = (ClientReadRequest) SerializationUtil
//					.deserialize(cm.getSerializedMessage());
//			// println(crr.getEncryptedBlindingFactor().toString());
//
//			CODEXServerMessage csm = new CODEXServerMessage(
//					new Random().nextLong(), getServerId(),
//					CODEXServerMessageType.READ_REQUEST,
//					SerializationUtil.serialize(cm));
//
//			println("Forwarding CLIENT_READ_REQUEST message to " + l
//					+ " servers");
//
//			// Make an entry in serverCheckCache of <nonce, MR(n)>
//			// serverCheckCache.put(csm.getNonce(), cm);
//
//			// Send messages to all servers
//			sendMessageToAllServers(csm);
//
//			// Now wait for a quorum number of messages
//			HashMap<SecretShare, Set<ForwardReadRequestAccept>> evidenceMap = new HashMap<SecretShare, Set<ForwardReadRequestAccept>>();
//			tu.reset();
//			int messagesReceived = 0;
//			while (tu.timerHasNotExpired()) {
//				CODEXServerMessage csmtemp;
//				try {
//					if (messagesReceived == quorumSize)
//						break;
//					csmtemp = serverMessages.poll(TimeUtility.timeOut,
//							TimeUnit.MILLISECONDS);
//					if (csmtemp == null) {
//						println("No message received. Timing out");
//						break;
//					}
//					if (csmtemp.getType().equals(
//							CODEXServerMessageType.READ_ACCEPT_RESPONSE)) {
//						ForwardReadRequestAccept frra = (ForwardReadRequestAccept) SerializationUtil
//								.deserialize(csmtemp.getSerializedMessage());
//
//						// Now check the signature of MR(n)
//						if (skm.verifyServerSignature(SerializationUtil
//								.serialize(cm), frra.getDigitalSig()
//								.toByteArray(), csmtemp.getSenderId())) {
//
//							// Now check the validity of the decrypted partial
//							// share
//							if (stkm.verifyDecryptedShare(frra.getCipher()
//									.toByteArray(), frra.getDecryptedShare())) {
//								SecretShare key = new SecretShare(
//										frra.getTimeStamp(), frra.getCipher());
//								Set<ForwardReadRequestAccept> tempevidenceSet = evidenceMap
//										.get(key);
//								if (tempevidenceSet == null) {
//									tempevidenceSet = new HashSet<ForwardReadRequestAccept>();
//									evidenceMap.put(key, tempevidenceSet);
//								}
//
//								tempevidenceSet.add(frra);
//								// if (tempevidenceSet.size() == t+1) {
//								// evidenceSet = tempevidenceSet;
//								// correctCipher = frra.getCipher();
//								// break;
//								// }
//
//								messagesReceived++;
//
//							}
//						}
//					}
//				} catch (InterruptedException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//
//			}
//
//			// There might be multiple sets with t+1 entries
//			// This might happen because the compromised servers might team up
//			// with servers with old state to send the old timestamp
//			// To break the tie, select the set with higher TimeStamp
//			Set<ForwardReadRequestAccept> evidenceSet = null;
//			BigInteger correctCipher = null;
//			BigInteger correctTimeStamp = null;
//			for (SecretShare key : evidenceMap.keySet()) {
//				Set<ForwardReadRequestAccept> frraSet = evidenceMap.get(key);
//				if (frraSet.size() >= t + 1) {
//					if (evidenceSet == null
//							|| (correctTimeStamp != null && key.getTimestamp()
//									.compareTo(correctTimeStamp) == 1)) {
//						evidenceSet = frraSet;
//						correctCipher = key.getSecret();
//						correctTimeStamp = key.getTimestamp();
//					}
//				}
//			}
//
//			if (evidenceSet == null) {
//				// Handle this later
//				System.out.println("Evidence set null");
//			} else {
//				println("MAJORITY : Got " + evidenceSet.size()
//						+ " messages with timestamp " + correctTimeStamp);
//				// REceived t+1 pieces of evidence
//
//				// Get t+1 sigshares to decrypt cipher
//				Set<SigShare> shares = new HashSet<SigShare>();
//
//				for (ForwardReadRequestAccept frra : evidenceSet) {
//					// if (frra.getDecryptedShare().getId() == 2
//					// || frra.getDecryptedShare().getId() == 4)
//					// shares[i++] = frra.getDecryptedShare();
//					shares.add(frra.getDecryptedShare());
//				}
//
//				BigInteger blindedSecret = stkm.thresholdDecrypt(correctCipher,
//						shares);
//
//				// Create the unsigned server response to be sent back to
//				// the client
//				BlindedReadResponse brr = new BlindedReadResponse(crr,
//						blindedSecret, crr.getDataId());
//
//				// Create the sign request
//				SignReadResponseRequest srrr = new SignReadResponseRequest(
//						evidenceSet, brr);
//
//				CODEXServerMessage csm2 = new CODEXServerMessage(
//						new Random().nextLong(), getServerId(),
//						CODEXServerMessageType.SIGN_READ_ACCEPT_RESPONSE,
//						SerializationUtil.serialize(srrr));
//
//				// Invoke a threshold signature protocol with all servers
//				// to sign the response message
//				sendMessageToAllServers(csm2);
//
//				BigInteger digitalSigRes = null;
//				Set<SigShare> sigs = new HashSet<SigShare>();
//				byte[] brrBytes = SerializationUtil.serialize(brr);
//				tu.reset();
//				while (tu.timerHasNotExpired()) {
//					CODEXServerMessage csmtemp;
//					try {
//						csmtemp = serverMessages.poll(TimeUtility.timeOut,
//								TimeUnit.MILLISECONDS);
//
//						if (csmtemp == null) {
//							println("No message received. Timing out");
//							break;
//						}
//
//						if (csmtemp.getType().equals(
//								CODEXServerMessageType.SIGNED_READ_ACCEPT_RESPONSE)
//								&& isNonceEqual(csm2, csmtemp)) {
//							SignedReadResponse srr = (SignedReadResponse) SerializationUtil
//									.deserialize(csmtemp.getSerializedMessage());
//
//							// Verify the signature on blinded response
//							if (stkm.verifySignedShare(brrBytes,
//									srr.getSignedShare())) {
//								sigs.add(srr.getSignedShare());
//								if (sigs.size() >= (t + 1)) {
//									println("Got t+1 signatures : "
//											+ sigs.size());
//									// Try generating the threshold signature
//									// Use the partial signatures to create the
//									// signed
//									// response for the client
//									digitalSigRes = stkm.thresholdSign(
//											brrBytes, sigs);
//									if (digitalSigRes == null) {
//										// Signature could not be generated
//										// So continue the loop and try one more
//										// signature
//									} else {
//										break;
//									}
//
//								}
//
//							} else {
//								println("Cannot verify blinded response");
//							}
//
//						}
//
//						// //
//						// println("Digital Signature on response message : "+digitalSigRes);
//						//
//						// // Verify the digital signature just in case
//						// if (!stkm.verifySignature(brrBytes, digitalSigRes)) {
//						// // Panic mode
//						// // The threshold generated signature on the response
//						// // message is incorrect
//						// }
//					} catch (InterruptedException e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					}
//
//				}
//
//				if (digitalSigRes == null) {
//					// Could not generate threshold signature
//					// Handle this later
//				} else {
//
//					CODEXClientMessage clientRes = new CODEXClientMessage(
//							cm.getNonce(), getServerId(),
//							CODEXClientMessageType.BLINDED_READ_RESPONSE,
//							brrBytes);
//					clientRes.setSerializedMessageSignature(digitalSigRes
//							.toByteArray());
//
//					// Send back response
//					println("Send back client reponse on host "
//							+ receivePacket.getAddress() + " and port "
//							+ receivePacket.getPort());
//					sendResponseMessage(clientRes, receivePacket.getAddress(),
//							receivePacket.getPort());
//				}
//
//			}
//
//		} else if (cm.getType().equals(
//				CODEXClientMessageType.CLIENT_UPDATE_REQUEST)) {
//
//			// Write Protocol starts here
//			ClientUpdateRequest crw = (ClientUpdateRequest) SerializationUtil
//					.deserialize(cm.getSerializedMessage());
//			// println(crr.getEncryptedBlindingFactor().toString());
//
//			// Request a timestamp from a quorum of servers
//			TimeStampRequest tsr = new TimeStampRequest(cm);
//
//			CODEXServerMessage csm_tsr = new CODEXServerMessage(
//					new Random().nextLong(), getServerId(),
//					CODEXServerMessageType.TIMESTAMP_REQUEST,
//					SerializationUtil.serialize(tsr));
//
//			println("Sending " + csm_tsr.getType() + " message to " + l
//					+ " servers");
//
//			sendMessageToAllServers(csm_tsr);
//
//			// Now wait for responses of type TIMESTAMP_RESPONSE from a quorum
//			// of servers
//			// Now wait for a quorum number of messages
//			HashMap<BigInteger, Set<TimeStampResponse>> evidenceMap = new HashMap<BigInteger, Set<TimeStampResponse>>();
//			tu.reset();
//			int messagesReceived = 0;
//			while (tu.timerHasNotExpired()) {
//				CODEXServerMessage csmtemp;
//				try {
//					if (messagesReceived == quorumSize)
//						break;
//					csmtemp = serverMessages.poll(TimeUtility.timeOut,
//							TimeUnit.MILLISECONDS);
//					if (csmtemp == null) {
//						println("No message received. Timing out");
//						break;
//					}
//					if (csmtemp.getType().equals(
//							CODEXServerMessageType.TIMESTAMP_RESPONSE)
//							&& isNonceEqual(csm_tsr, csmtemp)) {
//						TimeStampResponse tsres = (TimeStampResponse) SerializationUtil
//								.deserialize(csmtemp.getSerializedMessage());
//
//						// Create string with TIMESTAMP_RESPONSE
//						// Timestamp and MW(n)
//						String str = CODEXServerMessageType.TIMESTAMP_RESPONSE
//								+ tsres.getTimeStamp().toString()
//								+ (new BigInteger(
//										SerializationUtil.serialize(cm))
//										.toString());
//						// Now check the signature of MR(n)
////						if (skm.verifyServerSignature(SerializationUtil
////								.serialize(str), tsres.getDigitalSig()
////								.toByteArray(), csmtemp.getSenderId())) {
////
////							Set<TimeStampResponse> tempevidenceSet = evidenceMap
////									.get(tsres.getTimeStamp());
////							if (tempevidenceSet == null) {
////								tempevidenceSet = new HashSet<TimeStampResponse>();
////								evidenceMap.put(tsres.getTimeStamp(),
////										tempevidenceSet);
////							}
////
////							tempevidenceSet.add(tsres);
////							// if (tempevidenceSet.size() == t+1) {
////							// evidenceSet = tempevidenceSet;
////							// correctCipher = frra.getCipher();
////							// break;
////							// }
////
////							messagesReceived++;
////
////						}
//
//					}
//				} catch (InterruptedException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//
//			}
//
//			// There might be multiple sets with t+1 entries
//			// This might happen because the compromised servers might team up
//			// with servers with old state to send the old timestamp
//			// To break the tie, select the set with higher TimeStamp
//			Set<TimeStampResponse> evidenceSetTS = null;
//			BigInteger correctTimeStamp = null;
//			for (BigInteger key : evidenceMap.keySet()) {
//				Set<TimeStampResponse> tsSet = evidenceMap.get(key);
//				if (tsSet.size() >= t + 1) {
//					if (evidenceSetTS == null
//							|| (correctTimeStamp != null && key
//									.compareTo(correctTimeStamp) == 1)) {
//						evidenceSetTS = tsSet;
//						correctTimeStamp = key;
//					}
//				}
//			}
//
//			if (evidenceSetTS == null) {
//				// Handle this later
//				// No timestamp found
//				println("EvidenceSet for TimestampResponse is null");
//			}
//
//			ForwardWriteRequest fwr = new ForwardWriteRequest(cm,
//					correctTimeStamp.add(BigInteger.ONE), evidenceSetTS);
//
//			CODEXServerMessage csm = new CODEXServerMessage(
//					new Random().nextLong(), getServerId(),
//					CODEXServerMessageType.FORWARD_WRITE_REQUEST,
//					SerializationUtil.serialize(fwr));
//
//			println("Forwarding " + csm.getType() + " message to " + l
//					+ " servers");
//
//			// Send messages to all servers
//			sendMessageToAllServers(csm);
//
//			// Set<CODEXServerMessage> csmSet =
//			// messageCache.get(csm.getNonce());
//			Set<ForwardWriteRequestAccept> evidenceSet = new HashSet<ForwardWriteRequestAccept>();
//
//			// Wait for valid accepts from a quorum of servers
//			// or a timeout to occur
//			while (tu.timerHasNotExpired()) {
//				CODEXServerMessage csmtemp;
//				try {
//					csmtemp = serverMessages.poll(TimeUtility.timeOut,
//							TimeUnit.MILLISECONDS);
//
//					if (csmtemp == null) {
//						println("No message received. Timing out");
//						break;
//					}
//
//					if (csmtemp
//							.getType()
//							.equals(CODEXServerMessageType.FORWARD_WRITE_REQUEST_ACCEPT)
//							&& isNonceEqual(csm, csmtemp)) {
//						ForwardWriteRequestAccept frrw = (ForwardWriteRequestAccept) SerializationUtil
//								.deserialize(csmtemp.getSerializedMessage());
//
//						// Create string with FORWARD_WRITE_REQUEST_ACCEPT
//						// Timestamp and MW(n)
//						String str = CODEXServerMessageType.FORWARD_WRITE_REQUEST_ACCEPT
//								+ fwr.getTimestamp().toString()
//								+ (new BigInteger(
//										SerializationUtil.serialize(cm))
//										.toString());
//
//						// Now check the signature of MW(n)
//						if (skm.verifyServerSignature(SerializationUtil
//								.serialize(str), frrw.getDigitalSig()
//								.toByteArray(), csmtemp.getSenderId())) {
//
//							evidenceSet.add(frrw);
//							if (evidenceSet.size() == quorumSize) {
//								// Received 2t+1 pieces of evidence
//								break;
//							}
//						}
//					}
//
//				} catch (InterruptedException ie) {
//					ie.printStackTrace();
//				}
//			}
//
//			if (evidenceSet.size() < quorumSize) {
//				// Received less than quorum pieces of evidence
//				// Handle this later
//			} else {
//				// Received a quorum pieces of evidence
//
//				// Create the write reponse to be sent to client
//				ClientUpdateAcceptResponse wsr = new ClientUpdateAcceptResponse(crw,
//						crw.getDataId());
//
//				// Create the VERIFY message to be sent to other servers
//				VerifyWriteRequest vrw = new VerifyWriteRequest(crw, wsr,
//						evidenceSet, fwr.getTimestamp());
//
//				CODEXServerMessage csm_verify = new CODEXServerMessage(
//						new Random().nextLong(), getServerId(),
//						CODEXServerMessageType.VERIFY_WRITE_REQUEST,
//						SerializationUtil.serialize(vrw));
//
//				println("Sending " + csm_verify.getType() + " message to " + l
//						+ " servers");
//
//				// Send messages to all servers
//				sendMessageToAllServers(csm_verify);
//
//				tu.reset();
//
//				// Wait for valid verify from a quorum of servers
//				// or a timeout to occur
//				Set<VerifiedWriteRequest> verifiedEvidenceSet = new HashSet<VerifiedWriteRequest>();
//				while (tu.timerHasNotExpired()) {
//					CODEXServerMessage csmtemp;
//					try {
//						csmtemp = serverMessages.poll(TimeUtility.timeOut,
//								TimeUnit.MILLISECONDS);
//
//						if (csmtemp == null) {
//							println("No message received. Timing out");
//							break;
//						}
//
//						if (csmtemp.getType().equals(
//								CODEXServerMessageType.VERIFIED_WRITE_REQUEST)) {
//							VerifiedWriteRequest vdwr = (VerifiedWriteRequest) SerializationUtil
//									.deserialize(csmtemp.getSerializedMessage());
//
//							// Now check the signature of MW(n)
//							if (skm.verifyServerSignature(SerializationUtil
//									.serialize(wsr), vdwr.getDigitalSig()
//									.toByteArray(), csmtemp.getSenderId())) {
//
//								verifiedEvidenceSet.add(vdwr);
//								if (verifiedEvidenceSet.size() == quorumSize) {
//									// Received quorum pieces of evidence
//									break;
//								}
//
//							}
//						}
//
//					} catch (InterruptedException ie) {
//						ie.printStackTrace();
//					}
//				}
//
//				if (verifiedEvidenceSet.size() < quorumSize) {
//					// Received less than quorum pieces of evidence
//					// Handle this later
//				} else {
//
//					// Create the sign request
//					SignUpdateAcceptResponse swrr = new SignUpdateAcceptResponse(
//							verifiedEvidenceSet, wsr);
//
//					CODEXServerMessage csm_swrr = new CODEXServerMessage(
//							new Random().nextLong(), getServerId(),
//							CODEXServerMessageType.SIGN_UPDATE_ACCEPT_RESPONSE,
//							SerializationUtil.serialize(swrr));
//
//					// Invoke a threshold signature protocol with all
//					// servers
//					// to sign the response message
//					sendMessageToAllServers(csm_swrr);
//
//					tu.reset();
//					Set<SigShare> sigs = new HashSet<SigShare>();
//
//					byte[] wsrBytes = SerializationUtil.serialize(wsr);
//
//					BigInteger digitalSigRes = null;
//					while (tu.timerHasNotExpired()) {
//						CODEXServerMessage csmtemp;
//						try {
//							csmtemp = serverMessages.poll(TimeUtility.timeOut,
//									TimeUnit.MILLISECONDS);
//
//							if (csmtemp == null) {
//								println("No message received. Timing out");
//								break;
//							}
//
//							if (csmtemp
//									.getType()
//									.equals(CODEXServerMessageType.SIGNED_UPDATE_ACCEPT_RESPONSE)
//									&& csmtemp.getNonce() == csm_swrr
//											.getNonce()) {
//								SignedUpdateAcceptResponse swr = (SignedUpdateAcceptResponse) SerializationUtil
//										.deserialize(csmtemp
//												.getSerializedMessage());
//
//								// Verify the signature on write response
//								if (stkm.verifySignedShare(wsrBytes,
//										swr.getSignedShare())) {
//									sigs.add(swr.getSignedShare());
//									if (sigs.size() >= (t + 1)) {
//										println("Got t+1 signatures : "
//												+ sigs.size());
//										// Try generating the threshold
//										// signature
//										// Use the partial signatures to create
//										// the
//										// signed
//										// response for the client
//										digitalSigRes = stkm.thresholdSign(
//												wsrBytes, sigs);
//										if (digitalSigRes == null) {
//											// Signature could not be generated
//											// So continue the loop and try one
//											// more
//											// signature
//										} else {
//											break;
//										}
//
//									}
//								} else {
//									println("Cannot verify Write Response");
//								}
//
//							}
//
//						} catch (InterruptedException ie) {
//							ie.printStackTrace();
//						}
//					}
//
//					if (digitalSigRes == null) {
//						// Could not generate threshold signature
//						// Handle this later
//					} else {
//
//						CODEXClientMessage clientRes = new CODEXClientMessage(
//								cm.getNonce(), getServerId(),
//								CODEXClientMessageType.UPDATE_ACCEPT_RESPONSE,
//								wsrBytes);
//						clientRes.setSerializedMessageSignature(digitalSigRes
//								.toByteArray());
//
//						// Send back response
//						println("Send back client reponse on host "
//								+ receivePacket.getAddress() + " and port "
//								+ receivePacket.getPort());
//						sendResponseMessage(clientRes,
//								receivePacket.getAddress(),
//								receivePacket.getPort());
//
//					}
//				}
//
//			}
//		}
//
//	}
//
//	private void println(String string) {
//		long temptimer = System.currentTimeMillis();
//		// System.out.println("<"+dateFormatter.format(new Date())+">"+"Server "
//		// + this.getServerId() + " : " + string);
//		System.out.println("<" + (temptimer - timer) + ">" + "Server "
//				+ this.getServerId() + " : " + string);
//		timer = temptimer;
//
//	}
//
//	private void error(String string) {
//		long temptimer = System.currentTimeMillis();
//		// System.out.println("<"+dateFormatter.format(new Date())+">"+"Server "
//		// + this.getServerId() + " : " + string);
//		System.out.println("<" + (temptimer - timer) + ">" + "ERROR : Server "
//				+ this.getServerId() + " : " + string);
//		timer = temptimer;
//
//	}
//
//	// public void broadcastMessage(byte[] data) {
//	// InetAddress group;
//	// try {
//	// group = InetAddress.getByName(ReplicaServer.broadcastIP);
//	// DatagramPacket packet = new DatagramPacket(data, data.length,
//	// group, ReplicaServer.broadcastPort);
//	// this.clientConnectionSocket.send(packet);
//	// } catch (UnknownHostException e) {
//	// // TODO Auto-generated catch block
//	// e.printStackTrace();
//	// } catch (IOException e) {
//	// // TODO Auto-generated catch block
//	// e.printStackTrace();
//	// }
//	//
//	// }
//
//	// public boolean isMessageCount(String key, int count) {
//	// if (messageCache.containsKey(key)) {
//	// if (messageCache.get(key).size() >= count)
//	// return true;
//	// }
//	// return false;
//	// }
//
//	public void sendMessageToAllServers(CODEXServerMessage csm) {
//		// Clear the server messages queue
//		serverMessages.clear();
//
//		if (csm.getSerializedMessage() == null) {
//			return;
//		}
//
//		// Random r = new Random();
//		// Set<Integer> serversToSend = new HashSet<Integer>();
//		// while (serversToSend.size() != l) {
//		// serversToSend.add(r.nextInt(l));
//		// }
//		//
//		// for (Integer in : serversToSend) {
//		// sendMessage(csm, in);
//		// }
//
//		for (int i = 0; i < l; i++) {
//			sendMessage(csm, i);
//		}
//		// return serversToSend;
//	}
//
//	public long sendMessageToAllServers(InternalServerMessage ism,
//			CODEXServerMessageType csmt) {
//		println("Sending " + csmt + " message to " + l + " servers");
//		// Clear the server messages queue
//		serverMessages.clear();
//
//		if (ism == null || csmt == null) {
//			return -1;
//		}
//
//		long nonce = new Random().nextLong();
//
//		// Random r = new Random();
//		// Set<Integer> serversToSend = new HashSet<Integer>();
//		// while (serversToSend.size() != l) {
//		// serversToSend.add(r.nextInt(l));
//		// }
//		//
//		// for (Integer in : serversToSend) {
//		// sendMessage(csm, in);
//		// }
//
//		for (int i = 0; i < l; i++) {
//			ism.setDestinationId(i);
//			CODEXServerMessage csm = new CODEXServerMessage(nonce,
//					getServerId(), csmt, SerializationUtil.serialize(ism));
//			sendMessage(csm, i);
//		}
//		return nonce;
//	}
//
//	public void sendMessage(CODEXServerMessage csm, int serverId) {
//		println("Sending message of type " + csm.getType() + " to server "
//				+ serverId);
//		if (csm.getSerializedMessage() == null) {
//			return;
//		}
//
//		csm.setSenderId(getServerId());
//
//		// produce signature
//		skm.signMessage(csm);
//
//		byte[] dataToSend = SerializationUtil.serialize(csm);
//
//		DatagramPacket dp = new DatagramPacket(dataToSend, dataToSend.length,
//				serverConnectionInfo.get(serverId).getInetAddress(),
//				serverConnectionInfo.get(serverId).getPort());
//		// Send data over the channel to the delegate
//		// ccm.sendMessageSynchronous(dataToSend);
//
//		try {
//			serverConnectionSocket.send(dp);
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//	}
//
//	// public void sendMessage(InternalServerMessage ism, CODEXServerMessageType
//	// csmt, long nonce) {
//	// println("Sending message of type " + csmt + " to server "
//	// + serverId);
//	// if (ism == null || csmt == null) {
//	// return;
//	// }
//	//
//	// CODEXServerMessage csm = new CODEXServerMessage(nonce, getServerId(),
//	// csmt, )
//	// csm.setSenderId(getServerId());
//	//
//	// // produce signature
//	// skm.signMessage(csm);
//	//
//	// byte[] dataToSend = SerializationUtil.serialize(csm);
//	//
//	// DatagramPacket dp = new DatagramPacket(dataToSend, dataToSend.length,
//	// serverConnectionInfo.get(serverId).getInetAddress(),
//	// serverConnectionInfo.get(serverId).getPort());
//	// // Send data over the channel to the delegate
//	// // ccm.sendMessageSynchronous(dataToSend);
//	//
//	// try {
//	// serverConnectionSocket.send(dp);
//	// } catch (IOException e) {
//	// // TODO Auto-generated catch block
//	// e.printStackTrace();
//	// }
//	// }
//
//	public void sendResponseMessage(CODEXClientMessage ccm, InetAddress add,
//			int port) {
//		if (ccm.getSerializedMessage() == null
//				|| ccm.getSerializedMessageSignature() == null) {
//			return;
//		}
//
//		ccm.setSenderId(getServerId());
//
//		// not to produce signature since it should already be produced via
//		// threshold
//		// skm.signMessage(csm);
//
//		byte[] dataToSend = SerializationUtil.serialize(ccm);
//
//		DatagramPacket dp = new DatagramPacket(dataToSend, dataToSend.length,
//				add, port);
//		// Send data over the channel to the delegate
//		// ccm.sendMessageSynchronous(dataToSend);
//
//		try {
//			clientConnectionSocket.send(dp);
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//	}
//
//	private class ServerSocketListener implements Runnable {
//
//		@Override
//		public void run() {
//			// TODO Auto-generated method stub
//			println("Running server thread for server " + getServerId());
//			try {
//				serverConnectionSocket = new DatagramSocket(baseServerPort
//						+ getServerId());
//				// address = InetAddress.getByName(ReplicaServer.broadcastIP);
//				// socket.joinGroup(address);
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//
//			printState();
//
//			byte[] buf = new byte[16192];
//			DatagramPacket packet = new DatagramPacket(buf, buf.length);
//
//			while (true) {
//
//				try {
//					println("Waiting for server messages on socket : "
//							+ serverConnectionSocket.getLocalPort());
//					serverConnectionSocket.receive(packet);
//
//					handleServerPacket(packet);
//					// AcceptancePacket aPacket = AcceptancePacket
//					// .deserialize(packet.getData());
//					// Set<Integer> count = null;
//					// if (messageCache.containsKey(aPacket.toString())) {
//					// count = messageCache.get(aPacket.toString());
//					// } else {
//					// count = new HashSet<Integer>();
//					// }
//					// count.add((int) aPacket.getSenderId());
//					// messageCache.put(aPacket.toString(), count);
//
//					// printState();
//				} catch (IOException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//			}
//
//		}
//
//		private void handleServerPacket(DatagramPacket receivePacket) {
//
//			// System.out.println("Received a server message");
//			CODEXServerMessage sm = (CODEXServerMessage) SerializationUtil
//					.deserialize(receivePacket.getData());
//
//			println("Received " + sm.getType() + " from server "
//					+ sm.getSenderId() + " with nonce " + sm.getNonce());
//
//			// Verify the server signature and return if not valid
//			if (!skm.verifyServerSignature(sm)) {
//				System.out
//						.println("Cannot verify signature from server message");
//				return;
//			}
//
//			if (sm.getType()
//					.equals(CODEXServerMessageType.READ_REQUEST)) {
//				CODEXClientMessage ccm = (CODEXClientMessage) SerializationUtil
//						.deserialize(sm.getSerializedMessage());
//
//				// Verify client message
//				if (!skm.verifyClientSignature(ccm)) {
//					// Send a FORWARD_READ_REQUEST_REJECT
//					CODEXServerMessage csm = new CODEXServerMessage(
//							sm.getNonce(), getServerId(),
//							CODEXServerMessageType.FORWARD_READ_REQUEST_REJECT,
//							SerializationUtil.serialize(ccm));
//
//					sendMessage(csm, sm.getSenderId());
//				} else {
//					// Client Message is correct
//					ClientReadRequest crr = (ClientReadRequest) SerializationUtil
//							.deserialize(ccm.getSerializedMessage());
//
//					// Check if some value val(n) is locally bound to N
//					SecretShare ss = shareDB.get(crr.getDataId());
//
//					// If no value is bound, ignore the request
//					if (ss == null)
//						return;
//
//					// Compute blinded ciphertext c = E( val(N) * bp)
//					BigInteger cipher = ss.getSecret().multiply(
//							crr.getEncryptedBlindingFactor());
//
//					// BigInteger cipher = ss.getSecret();
//
//					// Compute the partial decryption along with the proof of
//					// validity
//					SigShare decryptedShare = stkm.decrypt(cipher);
//					println(decryptedShare.toString());
//
//					// Create the READ_ACCEPT_RESPONSE message to send
//					// back
//					ForwardReadRequestAccept frra = new ForwardReadRequestAccept(
//							new BigInteger(skm.getSignature(ccm)), cipher,
//							decryptedShare, ss.getTimestamp());
//
//					CODEXServerMessage csm = new CODEXServerMessage(
//							sm.getNonce(), getServerId(),
//							CODEXServerMessageType.READ_ACCEPT_RESPONSE,
//							SerializationUtil.serialize(frra));
//
//					sendMessage(csm, sm.getSenderId());
//				}
//
//			}
//			if (sm.getType().equals(
//					CODEXServerMessageType.FORWARD_TIMESTAMP_REQUEST)) {
//
//				TimeStampRequest tsr = (TimeStampRequest) SerializationUtil
//						.deserialize(sm.getSerializedMessage());
//
//				if (tsr.getDestinationId() != getServerId()) {
//					// Fatal
//					// Message was not meant for me
//					// Seems to be a replay
//					// Handle this later
//				}
//
//				CODEXClientMessage ccm = tsr.getCcm();
//
//				// Verify client message
//				if (!skm.verifyClientSignature(ccm)) {
//					// Send a FORWARD_READ_REQUEST_REJECT
//					CODEXServerMessage csm = new CODEXServerMessage(
//							sm.getNonce(),
//							getServerId(),
//							CODEXServerMessageType.FORWARD_TIMESTAMP_REQUEST_REJECT,
//							SerializationUtil.serialize(ccm));
//
//					sendMessage(csm, sm.getSenderId());
//				} else {
//					// Client Message is correct
//					ClientTimeStampRequest ctr = (ClientTimeStampRequest) SerializationUtil
//							.deserialize(ccm.getSerializedMessage());
//
//					BigInteger timestamp = null;
//
//					// Check if some value is locally bound to N
//					SecretShare ss = shareDB.get(ctr.getDataId());
//
//					// If no value is bound, then send back a one timestamp
//					if (ss == null)
//						timestamp = BigInteger.ONE;
//					else {
//						timestamp = ss.getTimestamp();
//					}
//
//					// // Create string with TIMESTAMP_RESPONSE
//					// // Timestamp and MW(n)
//					// String str = timestamp.toString()
//					// + (new BigInteger(SerializationUtil.serialize(ccm))
//					// .toString());
//
//					// Create the FORWARD_WRITE_REQUEST_ACCEPT message to send
//					// back
//					TimeStampResponse tsres = new TimeStampResponse(ccm,
//							timestamp, sm.getSenderId());
//
//					CODEXServerMessage csm = new CODEXServerMessage(
//							sm.getNonce(), getServerId(),
//							CODEXServerMessageType.FORWARD_TIMESTAMP_RESPONSE,
//							SerializationUtil.serialize(tsres));
//
//					sendMessage(csm, sm.getSenderId());
//
//				}
//
//			} else if (sm.getType().equals(
//					CODEXServerMessageType.SIGN_TIMESTAMP_ACCEPT_RESPONSE)) {
//
//				SignTimeStampResponseRequest strr = (SignTimeStampResponseRequest) SerializationUtil
//						.deserialize(sm.getSerializedMessage());
//
//				TimeStampReadResponse trr = strr.getTrr();
//
//				// Check the evidence set
//				Set<CODEXServerMessage> evidenceSetTS = strr.getEvidenceSet();
//				boolean checkFailed = false;
//				if (evidenceSetTS.size() < quorumSize) {
//					error("Evidence Set less than quorumSize : "+ evidenceSetTS.size() + " < "+ quorumSize);
//					checkFailed = true;
//				} else {
//
//					for (CODEXServerMessage csmtemp : evidenceSetTS) {
//						// First verify the signature
//						if (skm.verifyServerSignature(csmtemp)) {
//							// Next check the type of message
//							if (!csmtemp.getType().equals(
//									CODEXServerMessageType.FORWARD_TIMESTAMP_RESPONSE)) {
//								checkFailed = true;
//								error("Message type not as expected : "
//										+ CODEXServerMessageType.FORWARD_TIMESTAMP_RESPONSE
//										+ " vs " + csmtemp.getType());
//								break;
//							}
//
//							TimeStampResponse tsr = (TimeStampResponse) SerializationUtil
//									.deserialize(csmtemp.getSerializedMessage());
//
//							// CHeck if strr.getTrr().getCcm() == tsr.getCcm()
//							if(trr == null || tsr == null){
//								checkFailed = true;
//								error("Response to be signed null");
//								break;
//							}
//							
//							if (tsr.getDestinationId() != sm.getSenderId()) {
//								checkFailed = true;
//								error("Message was meant for "
//										+ tsr.getDestinationId() + " and not "
//										+ sm.getSenderId());
//								break;
//							}
//
//							if(trr.getCcm() == null || !trr.getCcm().equals(tsr.getCcm())){
//								error("Client messages dont match");
//								checkFailed = true;
//								break;
//							}
//							
//							
//								
//						}
//					}
//				}
//
//				if (checkFailed) {
//					// Dont send anything
//					// Add the server to compromised List
//					// Ignore future messages from server
//					println("Checking of evidence Set failed");
//				} else {
//
//					// At this time, the trr will be signed
//					SignedTimeStampResponse str = new SignedTimeStampResponse(
//							stkm.sign(SerializationUtil.serialize(trr)),
//							sm.getSenderId());
//
//					// println("Signed sig : " + srr.getSignedShare());
//					// Now create the response to be sent back
//					// The serialized message contains the brr
//					CODEXServerMessage csm = new CODEXServerMessage(
//							sm.getNonce(), getServerId(),
//							CODEXServerMessageType.SIGNED_TIMESTAMP_ACCEPT_RESPONSE,
//							SerializationUtil.serialize(str));
//
//					sendMessage(csm, sm.getSenderId());
//				}
//				// SigShare SignedRes = stkm.sign(brr);
//
//			} else if (sm.getType().equals(
//					CODEXServerMessageType.SIGN_READ_ACCEPT_RESPONSE)) {
//
//				SignReadResponseRequest srrr = (SignReadResponseRequest) SerializationUtil
//						.deserialize(sm.getSerializedMessage());
//
//				// Check the evidence set
//				// To be done later
//
//				BlindedReadResponse brr = srrr.getBrr();
//
//				// At this time, the brr will be signed
//				SignedReadResponse srr = new SignedReadResponse(
//						stkm.sign(SerializationUtil.serialize(brr)));
//
//				// println("Signed sig : " + srr.getSignedShare());
//				// Now create the response to be sent back
//				// The serialized message contains the brr
//				CODEXServerMessage csm = new CODEXServerMessage(sm.getNonce(),
//						getServerId(),
//						CODEXServerMessageType.SIGNED_READ_ACCEPT_RESPONSE,
//						SerializationUtil.serialize(srr));
//
//				sendMessage(csm, sm.getSenderId());
//
//				// SigShare SignedRes = stkm.sign(brr);
//
//			} else if (sm.getType().equals(
//					CODEXServerMessageType.READ_ACCEPT_RESPONSE)
//					|| sm.getType()
//							.equals(CODEXServerMessageType.FORWARD_WRITE_REQUEST_ACCEPT)
//					|| sm.getType().equals(
//							CODEXServerMessageType.TIMESTAMP_RESPONSE)
//					|| sm.getType().equals(
//							CODEXServerMessageType.SIGNED_UPDATE_ACCEPT_RESPONSE)
//					|| sm.getType().equals(
//							CODEXServerMessageType.SIGNED_READ_ACCEPT_RESPONSE)
//					|| sm.getType().equals(
//							CODEXServerMessageType.VERIFIED_WRITE_REQUEST)
//					|| sm.getType().equals(
//							CODEXServerMessageType.FORWARD_TIMESTAMP_RESPONSE)
//					|| sm.getType().equals(
//							CODEXServerMessageType.SIGNED_TIMESTAMP_ACCEPT_RESPONSE)
//					|| sm.getType()
//							.equals(CODEXServerMessageType.FORWARD_TIMESTAMP_REQUEST_REJECT)) {
//
//				println("Added a CODEX_SERVER_MESSAGE:" + sm.getType()
//						+ " in message queue with nonce " + sm.getNonce());
//				serverMessages.add(sm);
//
//				// Object objectToCheck = serverCheckCache.get(sm.getNonce());
//				// if (objectToCheck == null) {
//				// println("No object to check the message against. Ignore message");
//				// } else {
//				//
//				// ForwardReadRequestAccept frra = (ForwardReadRequestAccept)
//				// SerializationUtil
//				// .deserialize(sm.getSerializedMessage());
//				//
//				// // Now check the signature of MR(n)
//				// if (skm.verifyServerSignature(SerializationUtil
//				// .serialize(objectToCheck), frra.getDigitalSig()
//				// .toByteArray(), sm.getSenderId())) {
//				//
//				// // Now check the validity of the decrypted partial
//				// // share
//				// if (stkm.verifyDecryptedShare(frra.getCipher()
//				// .toByteArray(), frra.getDecryptedShare())) {
//				//
//				// // The message is verified and is valid
//				// println("Verified and Added a CODEX_SERVER_MESSAGE:"
//				// + sm.getType()
//				// + " in message queue with nonce "
//				// + sm.getNonce());
//				// // Add the message to the message cache
//				// serverMessages.add(sm);
//				//
//				// // Set<CODEXServerMessage> messageSet = messageCache
//				// // .get(sm.getNonce());
//				// // if (messageSet != null
//				// // && messageSet.size() == (2 * t + 1)) {
//				// // // Already received a quorum of correct
//				// // // messages, so ignore this message
//				// // println("Ignoring the message");
//				// // } else {
//				// //
//				// // if (messageSet == null) {
//				// // // messageSet =
//				// // // Collections.synchronizedSet(new
//				// // // HashSet<CODEXServerMessage>());
//				// // messageSet = new HashSet<CODEXServerMessage>();
//				// // messageCache.put(sm.getNonce(), messageSet);
//				// // }
//				// // messageSet.add(sm);
//				// // println("Message Set size : "
//				// // + messageSet.size());
//				// // }
//				// }
//				// }
//				// }
//				//
//				// } else if (sm.getType().equals(
//				// CODEXServerMessageType.FORWARD_WRITE_REQUEST_ACCEPT)
//				// || sm.getType().equals(
//				// CODEXServerMessageType.SIGNED_WRITE_RESPONSE)
//				// || sm.getType().equals(
//				// CODEXServerMessageType.SIGNED_READ_ACCEPT_RESPONSE)
//				// || sm.getType().equals(
//				// CODEXServerMessageType.VERIFIED_WRITE_REQUEST)) {
//				//
//				// println("Added a CODEX_SERVER_MESSAGE:" + sm.getType()
//				// + " in message queue with nonce " + sm.getNonce());
//				// // Add the message to the message cache
//				// Set<CODEXServerMessage> messageSet = messageCache.get(sm
//				// .getNonce());
//				// if (messageSet == null) {
//				// // messageSet = Collections.synchronizedSet(new
//				// // HashSet<CODEXServerMessage>());
//				// messageSet = new HashSet<CODEXServerMessage>();
//				// messageCache.put(sm.getNonce(), messageSet);
//				// }
//				// messageSet.add(sm);
//				// println("Message Set size : " + messageSet.size());
//
//			} else if (sm.getType().equals(
//					CODEXServerMessageType.FORWARD_WRITE_REQUEST)) {
//
//				ForwardWriteRequest fwr = (ForwardWriteRequest) SerializationUtil
//						.deserialize(sm.getSerializedMessage());
//
//				CODEXClientMessage ccm = fwr.getCwr();
//
//				// Verify client message
//				if (!skm.verifyClientSignature(ccm)) {
//					// Send a FORWARD_WRITE_REQUEST_REJECT
//					CODEXServerMessage csm = new CODEXServerMessage(
//							sm.getNonce(),
//							getServerId(),
//							CODEXServerMessageType.FORWARD_WRITE_REQUEST_REJECT,
//							SerializationUtil.serialize(ccm));
//
//					sendMessage(csm, sm.getSenderId());
//				} else {
//					// Verify the evidence Set
//					// Do this later
//
//					// Client Message is correct
//					ClientUpdateRequest crw = (ClientUpdateRequest) SerializationUtil
//							.deserialize(ccm.getSerializedMessage());
//
//					// ?? Locally bind E(s) to name N ??
//					println("Updating dataId " + crw.getDataId()
//							+ " with timestamp " + fwr.getTimestamp()
//							+ " and secret " + crw.getEncryptedSecret());
//					shareDB.put(
//							crw.getDataId(),
//							new SecretShare(fwr.getTimestamp(), crw
//									.getEncryptedSecret()));
//
//					// Create string with FORWARD_WRITE_REQUEST_ACCEPT
//					// Timestamp and MW(n)
//					String str = CODEXServerMessageType.FORWARD_WRITE_REQUEST_ACCEPT
//							+ fwr.getTimestamp().toString()
//							+ (new BigInteger(SerializationUtil.serialize(ccm))
//									.toString());
//
//					// Create the FORWARD_WRITE_REQUEST_ACCEPT message to send
//					// back
//					ForwardWriteRequestAccept frrw = new ForwardWriteRequestAccept(
//							new BigInteger(skm.getSignature(str)));
//
//					CODEXServerMessage csm = new CODEXServerMessage(
//							sm.getNonce(),
//							getServerId(),
//							CODEXServerMessageType.FORWARD_WRITE_REQUEST_ACCEPT,
//							SerializationUtil.serialize(frrw));
//
//					sendMessage(csm, sm.getSenderId());
//				}
//
//			} else if (sm.getType().equals(
//					CODEXServerMessageType.VERIFY_WRITE_REQUEST)) {
//
//				VerifyWriteRequest vwr = (VerifyWriteRequest) SerializationUtil
//						.deserialize(sm.getSerializedMessage());
//
//				// the signature of sender Server is already verified above
//
//				// Verify the evidence Set Ed
//				// To do later
//				// Just do basic check to see if size is at least 2t+1
//				if (vwr.getEvidenceSet().size() < (2 * t + 1)) {
//					// Evidence set check fail
//					// Send REJECT message
//					// later
//
//				} else {
//					// Evidence set check passes
//
//					// Bind E(s) from MW(n) to name N
//					ClientUpdateRequest cwr = vwr.getCwr();
//
//					// ?? Verify signature of MW(n) here ??
//					BigInteger ts = null;
//					// if (shareDB.containsKey(cwr.getDataId())) {
//					// ts = shareDB.get(cwr.getDataId()).getTimestamp();
//					// } else {
//					// ts = BigInteger.ZERO;
//					// }
//
//					// Update db here
//					println("Updating dataId " + cwr.getDataId()
//							+ " with timestamp " + vwr.getTimestamp()
//							+ " and secret " + cwr.getEncryptedSecret());
//
//					shareDB.put(
//							cwr.getDataId(),
//							new SecretShare(vwr.getTimestamp(), cwr
//									.getEncryptedSecret()));
//
//					// Send VERIFIED message back to sender Server
//					VerifiedWriteRequest vdwr = new VerifiedWriteRequest(
//							new BigInteger(skm.getSignature(vwr.getWsr())));
//
//					CODEXServerMessage csm = new CODEXServerMessage(
//							sm.getNonce(), getServerId(),
//							CODEXServerMessageType.VERIFIED_WRITE_REQUEST,
//							SerializationUtil.serialize(vdwr));
//
//					sendMessage(csm, sm.getSenderId());
//
//				}
//
//			} else if (sm.getType().equals(
//					CODEXServerMessageType.SIGN_UPDATE_ACCEPT_RESPONSE)) {
//
//				SignUpdateAcceptResponse swrr = (SignUpdateAcceptResponse) SerializationUtil
//						.deserialize(sm.getSerializedMessage());
//
//				// Check the evidence set
//				// To be done later
//
//				ClientUpdateAcceptResponse wsr = swrr.getWsr();
//
//				// At this time, the wsr will be signed
//				SignedUpdateAcceptResponse swr = new SignedUpdateAcceptResponse(
//						stkm.sign(SerializationUtil.serialize(wsr)));
//
//				// println("Signed sig : " + srr.getSignedShare());
//				// Now create the response to be sent back
//				// The serialized message contains the brr
//				CODEXServerMessage csm = new CODEXServerMessage(sm.getNonce(),
//						getServerId(),
//						CODEXServerMessageType.SIGNED_UPDATE_ACCEPT_RESPONSE,
//						SerializationUtil.serialize(swr));
//
//				sendMessage(csm, sm.getSenderId());
//
//				// SigShare SignedRes = stkm.sign(brr);
//
//			}
//		}
//	}
//
//	public void printState() {
//		println("Current state of server " + getServerId() + " :");
//		// for (Long s : messageCache.keySet()) {
//		// System.out.println(s + " --  ");
//		// for (CODEXServerMessage i : messageCache.get(s)) {
//		// System.out.print(i + " ");
//		// }
//		// System.out.println();
//		// }
//
//		for (String s : shareDB.keySet()) {
//			System.out.println(s + " : " + shareDB.get(s));
//		}
//	}
//
//	public int getServerId() {
//		return serverId;
//	}
//
//	public void setServerId(int serverId) {
//		this.serverId = serverId;
//	}
//
//	public void addSecret(String id, SecretShare ss) {
//		this.shareDB.put(id, ss);
//		printState();
//	}
//
//	public boolean isNonceEqual(CODEXServerMessage csm1, CODEXServerMessage csm2) {
//		if (csm1 == null)
//			return false;
//		if (csm2 == null)
//			return false;
//		return csm1.getNonce() == csm2.getNonce();
//	}
//}
