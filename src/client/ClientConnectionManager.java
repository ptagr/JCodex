package client;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import client.messages.CODEXClientMessage;
import client.messages.CODEXClientMessageType;

import utils.ClientTimeUtility;
import utils.SerializationUtil;
import utils.TimeUtility;

import main.ConnectionInfo;

/*
 * Responsible for establishing connections with the servers
 * and sending message to them
 * Also manages the client sockets
 */
public class ClientConnectionManager {

	private int l;
	private int k;
	private Map<Integer, ConnectionInfo> serverConnectionInfo;
	private DatagramSocket clientSocket;

	ClientConnectionManager(int k, int l, int clientSocketPort,
			int serverSocketPort) {
		this.k = k;
		this.l = l;

		try {
			this.clientSocket = new DatagramSocket(clientSocketPort);
			this.clientSocket.setSoTimeout(ClientTimeUtility.timeOut);
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		serverConnectionInfo = new HashMap<Integer, ConnectionInfo>();
		for (int i = 0; i < l; i++) {
			serverConnectionInfo.put(i, new ConnectionInfo("localhost",
					serverSocketPort + i));
		}
	}

	// public void establishLocalConnections(int basePort){
	//
	// }

	public void sendMessage(byte[] data) {

		// Select a delegate at random
		int delegateId = new Random().nextInt(l);

		// Get connection info for that delegate
		ConnectionInfo ci = serverConnectionInfo.get(delegateId);

		// Create the datagram packet to send
		DatagramPacket dp = new DatagramPacket(data, data.length,
				ci.getInetAddress(), ci.getPort());
		try {

			System.out.println("Sending a message to server " + delegateId
					+ " on port " + ci.getPort());
			// Send the datagram packet through the client socket
			this.clientSocket.send(dp);

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public CODEXClientMessage getNextMessageSynchronous() throws IOException {
		// Wait for a message of type BLINDED_READ_RESPONSE from the server
		// Discard any other message
		byte[] receiveData = new byte[8192];
		while (true) {
			DatagramPacket receivePacket = new DatagramPacket(receiveData,
					receiveData.length);
			this.clientSocket.receive(receivePacket);

			CODEXClientMessage cm = (CODEXClientMessage) SerializationUtil
					.deserialize(receivePacket.getData());

			return cm;
		}
	}
}
