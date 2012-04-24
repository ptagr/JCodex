package main;

import java.io.Serializable;

public class CODEXMessage implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7138906906495456742L;

	private long nonce; // Nonce number defined by the client

	private int senderId; // Source of the message

	// private transient byte[] content = null; // Content of the message

	// the bytes received from the client and its MAC and signature
	private byte[] serializedMessage = null;
	private byte[] serializedMessageSignature = null;

	// public transient byte[] serializedMessageMAC = null;

	// public CODEXMessage(byte[] serializedMsg) {
	// ByteArrayInputStream bais = new ByteArrayInputStream(serializedMsg);
	// DataInputStream dis = new DataInputStream(bais);
	// try {
	// rExternal(dis);
	// } catch (IOException e) {
	// // TODO Auto-generated catch block
	// e.printStackTrace();
	// } catch (ClassNotFoundException e) {
	// // TODO Auto-generated catch block
	// e.printStackTrace();
	// }
	// }

	public CODEXMessage(long nonce, int senderId, byte[] serializedMsg) {
		this.nonce = nonce;
		this.senderId = senderId;
		this.setSerializedMessage(serializedMsg);

	}

	public CODEXMessage(int nonce) {
		// TODO Auto-generated constructor stub

		this.nonce = nonce;
	}

	public long getNonce() {
		return nonce;
	}

	public void setNonce(long nonce) {
		this.nonce = nonce;
	}

	public byte[] getSerializedMessage() {
		// if (serializedMessage == null) {
		// DataOutputStream dos = null;
		// try {
		// ByteArrayOutputStream baos = new ByteArrayOutputStream();
		// dos = new DataOutputStream(baos);
		// wExternal(dos);
		// dos.flush();
		// setSerializedMessage(baos.toByteArray());
		// } catch (IOException ie) {
		// ie.printStackTrace();
		// }
		// }
		return serializedMessage;
	}

	public void setSerializedMessage(byte[] serializedMessage) {
		this.serializedMessage = serializedMessage.clone();

		// Make the signature null because it might not correspond
		this.serializedMessageSignature = null;
	}

	public byte[] getSerializedMessageSignature() {
		return serializedMessageSignature;
	}

	public void setSerializedMessageSignature(byte[] serializedMessageSignature) {
		this.serializedMessageSignature = serializedMessageSignature.clone();
	}

	public int getSenderId() {
		return senderId;
	}

	public void setSenderId(int senderId) {
		this.senderId = senderId;
	}

}
