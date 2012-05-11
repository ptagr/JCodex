package proactive.messages;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.HashMap;

import server.SecretShare;

public class SendReadyMessage extends InternalPRSServerMessage implements
		Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8794330652213445246L;

	private int senderId;

	public SendReadyMessage(int senderId) {
		super();

		this.senderId = senderId;
	}

	public int getSenderId() {
		return senderId;
	}

	public void setSenderId(int senderId) {
		this.senderId = senderId;
	}
}
