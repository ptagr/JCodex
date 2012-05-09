package proactive.messages;

import java.io.Serializable;
import java.net.InetAddress;

public class PRSMessage implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6128973433335356813L;

	/**
	 * 
	 */

	private PRSMessageType type;
	private InetAddress address;
	private int port;
	private long nonce; // Nonce number defined by the client

	// private transient byte[] content = null; // Content of the message

	// the bytes received from the client and its MAC and signature
	private byte[] serializedMessage = null;
	private byte[] serializedMessageSignature = null;

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

	public PRSMessage(long nonce, PRSMessageType cmt, byte[] serializedMsg) {
		this.nonce = nonce;
		// this.senderId = senderId;
		this.setSerializedMessage(serializedMsg);
		this.type = cmt;

	}

	public PRSMessage(int nonce) {
		this.nonce = nonce;
	}

	public PRSMessageType getType() {
		return type;
	}

	public void setType(PRSMessageType type) {
		this.type = type;
	}

	public InetAddress getAddress() {
		return address;
	}

	public void setAddress(InetAddress address) {
		this.address = address;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

}
