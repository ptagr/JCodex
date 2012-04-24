//package server;
//
//import java.io.IOException;
//import java.net.BindException;
//import java.net.DatagramPacket;
//import java.net.DatagramSocket;
//import java.net.InetAddress;
//import java.net.MulticastSocket;
//import java.net.UnknownHostException;
//
//import java.sql.Timestamp;
//import java.util.HashMap;
//import java.util.HashSet;
//import java.util.LinkedHashMap;
//import java.util.Random;
//import java.util.Set;
//
//import client.messages.CODEXClientMessage;
//
//import main.AccessOrderCache;
//
//import utils.SerializationUtil;
//
//public class ReplicaServer implements Runnable {
//	public static int baseSocket = 10000;
//	public static String broadcastIP = "230.0.0.1";
//	public static int broadcastPort = 4446;
//	public static int timeOutForAcceptanceMessages = 10000; // time in mSec
//	//public static int writeQuorumCount = 3 * DataPlaneNode.b + 1;
//
//	public static int[] serverPorts = { 9001, 9002, 9003, 9004, 9005 };
//	private int id;
//
//	private Thread bclThread;
//	private HashMap<String, SecretShare> shareDB;
//	private DatagramSocket serverSocket;
//
//	LinkedHashMap<String, Set<Integer>> messageCache = new AccessOrderCache<String, Set<Integer>>(
//			50);
//
//	public ReplicaServer(int id) {
//		super();
//		this.setId(id);
//		this.shareDB = new HashMap<String, SecretShare>();
//
//		this.bclThread = new Thread(new BroadcastChannelListener());
//		bclThread.start();
//		try {
//			System.out.println("Starting server " + id + " at port "
//					+ (ReplicaServer.baseSocket + id));
//			this.serverSocket = new DatagramSocket(ReplicaServer.baseSocket
//					+ id);
//		} catch (IOException e) {
//			if (e instanceof BindException) {
//				System.out.println("Port already in use");
//			}
//			e.printStackTrace();
//		}
//	}
//
//	@Override
//	public void run() {
//		// TODO Auto-generated method stub
//		byte[] receiveData = new byte[8192];
//		while (true) {
//			DatagramPacket receivePacket = new DatagramPacket(receiveData,
//					receiveData.length);
//			try {
//				serverSocket.receive(receivePacket);
//				handlePacket(receivePacket);
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		}
//
//	}
//	
//	
//	private void checkSignatureOnPacket(byte[] packet){
//		CODEXClientMessage cm = (CODEXClientMessage) SerializationUtil.deserialize(packet);
//		
//	}
//	
//
//	private void handlePacket(DatagramPacket receivePacket) {
//		// TODO Auto-generated method stub
//		byte[] data = receivePacket.getData();
//		// byte[] responseData = new byte[1024];
//
//		CODEXClientMessage cm = null;
//
//		try {
//			dataPacket = SerializationUtil.deserialize(data);
//			DataPacket response = null;
//			if (dataPacket.getPacketType() == DataPacket.PacketType.TIMESTAMP_REQ) {
//				System.out.println("Server " + this.getId()
//						+ ": Received Req TIMESTAMP_REQ from client "
//						+ dataPacket.getSenderId() + " for pubId "
//						+ dataPacket.getDataId());
//				String dataId = dataPacket.getDataId();
//				response = new DataPacket(
//						DataPacket.PacketType.TIMESTAMP_RESPONSE, this.getId(),
//						new Random().nextLong(), dataPacket.getDataId());
//				response.setOrigRequest(data);
//				ShareInfo shareInfo;
//
//				if (!shareDB.containsKey(dataId)) {
//					shareInfo = new ShareInfo(new Timestamp(0));
//					//shareDB.put(dataId, shareInfo);
//				} else {
//					shareInfo = new ShareInfo(shareDB.get(dataId)
//							.getTimeStamp());
//				}
//				response.setShareInfo(shareInfo);
//
//			} else if (dataPacket.getPacketType().equals(
//					DataPacket.PacketType.SHARE_SEND)) {
//				System.out.println("Server " + this.getId()
//						+ ": Received Req SHARE_SEND from client "
//						+ dataPacket.getSenderId() + " for pubId "
//						+ dataPacket.getDataId());
//				String dataId = dataPacket.getDataId();
//				ShareInfo shareInfo = shareDB.get(dataId);
//
//				// If shareInfo is null, it is possible that this server didnot
//				// participate in sending the timestamp for the first time
//				if (shareInfo == null) {
//					System.out.println("Unknown dataId " + dataId
//							+ ". This server didnot participate in the TS_REQ phase !!");
//				} else {
//
//					// Check 1: If the new timestamp is newer than the stored
//					// timestamp
//					Timestamp oldTS = shareInfo.getTimeStamp();
//					if (dataPacket.getShareInfo().getTimeStamp().before(oldTS)) {
//						System.out
//								.println("New TS older than old TS. Ignoring the request.");
//						return;
//					}
//				}
//
//				// Check 2: Verify if the share received is valid
//				if (!dataPacket.getShareInfo().getShare()
//						.verifyShare(this.getId())) {
//					System.out.println("Given share is not valid");
//					return;
//				}
//
//				// Broadcast an acceptance containing the timestamp and the hash
//				// of all the witnesses
//				AcceptancePacket aPacket = new AcceptancePacket(
//						DataPacket.PacketType.ACCEPTANCE_BROADCAST,
//						this.getId(), new Random().nextLong(), dataPacket
//								.getShareInfo().getTimeStamp(), dataPacket
//								.getShareInfo().getShare()
//								.generateWitnessesHash(), dataId);
//
//				broadcastMessage(DataPacket.serialize(aPacket));
//
//				// Wait for a write quorum of servers to find the request with
//				// the same set of witnesses and timestamp valid
//				// Wait for timeout number of milliseconds
//				long currentTime = System.currentTimeMillis();
//				while (!isMessageCount(aPacket.toString(),
//						ReplicaServer.writeQuorumCount)
//						&& ((System.currentTimeMillis() - currentTime) < ReplicaServer.timeOutForAcceptanceMessages)) {
//					System.out
//							.println("Wait for a write quorum of acceptance messages");
//					try {
//						Thread.sleep(1000);
//					} catch (InterruptedException e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					}
//				}
//
//				if (isMessageCount(aPacket.toString(),
//						ReplicaServer.writeQuorumCount)) {
//					System.out
//							.println("Received a write quorum of acceptance messages");
//
//					//Record the identities of other servers in the write quorum
//					dataPacket.getShareInfo().addAllServerIds(messageCache.get(aPacket.toString()));
//					
//					
//					messageCache.remove(aPacket.toString());
//
//					// Replace current share, witnesses and timestamp with the
//					// new
//					// values
//					System.out
//							.println("Updating current share, witnesses and timestamp of "
//									+ dataId);
//					shareDB.put(dataId, dataPacket.getShareInfo());
//					return;
//
//				} else {
//					System.out
//							.println("Did not receive a write quorum of acceptance messages within the timeout period");
//					messageCache.remove(aPacket.toString());
//					return;
//				}
//
//			} else if (dataPacket.getPacketType().equals(
//					DataPacket.PacketType.SHARE_REQUEST)) {
//				System.out.println("Server " + this.getId()
//						+ ": Received Req SHARE_REQUEST from client "
//						+ dataPacket.getSenderId() + " for pubId "
//						+ dataPacket.getDataId());
//
//				// Check authrization of subscriber here
//				// To be implemented
//
//				String dataId = dataPacket.getDataId();
//				response = new DataPacket(DataPacket.PacketType.SHARE_RESPONSE,
//						this.getId(), new Random().nextLong(), dataId);
//				response.setOrigRequest(data);
//				ShareInfo shareInfo;
//
//				if (!shareDB.containsKey(dataId)) {
//					System.out.println("Error!! No secret with pubId " + dataId
//							+ " found");
//					return;
//				} else {
//					shareInfo = new ShareInfo(shareDB.get(dataId).getShare(),
//							shareDB.get(dataId).getTimeStamp());
//					response.setShareInfo(shareInfo);
//					response.setDataId(dataId);
//				}
//
//			}
//
//			InetAddress IPAddress = receivePacket.getAddress();
//			int port = receivePacket.getPort();
//			byte[] responseData = DataPacket.serialize(response);
//			System.out.println("Response Packet Size : " + responseData.length);
//			DatagramPacket responsePacket = new DatagramPacket(responseData,
//					responseData.length, IPAddress, port);
//
//			this.serverSocket.send(responsePacket);
//
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (ClassNotFoundException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//
//	}
//
//	public void broadcastMessage(byte[] data) {
//		InetAddress group;
//		try {
//			group = InetAddress.getByName(ReplicaServer.broadcastIP);
//			DatagramPacket packet = new DatagramPacket(data, data.length,
//					group, ReplicaServer.broadcastPort);
//			this.serverSocket.send(packet);
//		} catch (UnknownHostException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//
//	}
//
//	public int getId() {
//		return id;
//	}
//
//	public void setId(int id) {
//		this.id = id;
//	}
//
//	public boolean isMessageCount(String key, int count) {
//		if (messageCache.containsKey(key)) {
//			if (messageCache.get(key).size() >= count)
//				return true;
//		}
//		return false;
//	}
//
//	// public static void main(String args[]) {
//	// for (int i = 1; i <= 4; i++) {
//	// ReplicaServer rs = new ReplicaServer(i);
//	// new Thread(rs).start();
//	// }
//	// }
//
//	private class BroadcastChannelListener implements Runnable {
//
//		MulticastSocket socket;
//		InetAddress address;
//
//		@Override
//		public void run() {
//			// TODO Auto-generated method stub
//			System.out.println("Running broadcast channel thread for server "
//					+ getId());
//			try {
//				socket = new MulticastSocket(ReplicaServer.broadcastPort);
//				address = InetAddress.getByName(ReplicaServer.broadcastIP);
//				socket.joinGroup(address);
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//
//			printState();
//
//			byte[] buf = new byte[8192];
//			DatagramPacket packet = new DatagramPacket(buf, buf.length);
//
//			while (true) {
//
//				try {
//					socket.receive(packet);
//
//					AcceptancePacket aPacket = AcceptancePacket
//							.deserialize(packet.getData());
//					Set<Integer> count = null;
//					if (messageCache.containsKey(aPacket.toString())) {
//						count = messageCache.get(aPacket.toString());
//					} else {
//						count = new HashSet<Integer>();
//					}
//					count.add((int) aPacket.getSenderId());
//					messageCache.put(aPacket.toString(), count);
//					printState();
//				} catch (IOException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				} catch (ClassNotFoundException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//			}
//
//		}
//
//		public void printState() {
//			System.out.println("Current state of server " + getId() + " :");
//			for (String s : messageCache.keySet()) {
//				System.out.println(s + " --  ");
//				for (Integer i : messageCache.get(s)) {
//					System.out.print(i + " ");
//				}
//				System.out.println();
//			}
//		}
//
//	}
//
//	public static void main(String args[]) {
//		if (args.length < 1) {
//			System.out.println("Usage : ReplicaServer <id>");
//			return;
//		}
//
//		ReplicaServer rs = new ReplicaServer(Integer.parseInt(args[0]));
//		new Thread(rs).start();
//
//	}
//
//}
