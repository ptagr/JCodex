package client.messages;

import java.io.Serializable;
import java.math.BigInteger;
import java.net.InetAddress;
import java.util.Arrays;

import main.CODEXMessage;

public class CODEXClientMessage extends CODEXMessage implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7138906906495456742L;

	private CODEXClientMessageType type;
	
	private InetAddress address;
	private int port;

	public CODEXClientMessage(long nonce, int senderId, CODEXClientMessageType cmt,
			byte[] serializedMsg) {
		super(nonce, senderId, serializedMsg);
		this.type = cmt;

	}

	public CODEXClientMessage(int nonce) {
		super(nonce);
	}

	public CODEXClientMessageType getType() {
		return type;
	}

	public void setType(CODEXClientMessageType type) {
		this.type = type;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if ((obj == null) || (obj.getClass() != this.getClass()))
			return false;
		CODEXClientMessage ccm = (CODEXClientMessage) obj;
		return this.getNonce() == ccm.getNonce() &&
				this.getSenderId() == ccm.getSenderId() &&
				this.getType().equals(ccm.getType()) &&
				Arrays.equals(this.getSerializedMessage(), ccm.getSerializedMessage());
	}
	@Override
	public int hashCode() {
		// TODO Auto-generated method stub
		int hash = 7;
		hash = (int) (31 * hash + this.getNonce()%hash);
		hash = 31 * hash + this.getSenderId();
		hash = 31 * hash + new BigInteger(getSerializedMessage()).intValue();
		return hash;
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
