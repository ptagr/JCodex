package server.messages;

import java.io.Serializable;

import main.CODEXMessage;

public class CODEXServerMessage extends CODEXMessage implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7138906906495456742L;

	private CODEXServerMessageType type;

	public CODEXServerMessage(long nonce, int senderId, CODEXServerMessageType cmt,
			byte[] serializedMsg) {
		super(nonce, senderId, serializedMsg);
		this.type = cmt;

	}

	public CODEXServerMessage(int nonce) {
		super(nonce);
	}

	public CODEXServerMessageType getType() {
		return type;
	}

	public void setType(CODEXServerMessageType type) {
		this.type = type;
	}

}
