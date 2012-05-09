package proactive.messages;

import java.io.Serializable;

public class PRSServerMessage implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5361685064460630917L;

	private long nonce; // Nonce number defined by the client

	private int senderId; // Source of the message

	private PRSMessageType type;

	// the bytes received from the client and its MAC and signature
	private byte[] serializedMessage = null;
	private byte[] serializedMessageSignature = null;

	public PRSServerMessage(long nonce, int senderId, byte[] serializedMsg,
			PRSMessageType type) {
		this.nonce = nonce;
		this.senderId = senderId;
		this.setSerializedMessage(serializedMsg);
		this.setType(type);
	}

	public PRSServerMessage(int nonce) {
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

	public PRSMessageType getType() {
		return type;
	}

	public void setType(PRSMessageType type) {
		this.type = type;
	}

}
