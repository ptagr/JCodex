package client.messages;

import java.io.Serializable;

import main.CODEXMessage;

public class CODEXClientMessage extends CODEXMessage implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7138906906495456742L;

	private CODEXClientMessageType type;

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

}
