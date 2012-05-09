package proactive.messages;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.HashMap;

import server.SecretShare;

public class SendSecretMessage extends InternalPRSServerMessage implements
		Serializable {



	/**
	 * 
	 */
	private static final long serialVersionUID = 8794330652213445246L;

	// Secret generated by the sending server
	private BigInteger secret;

	private int senderId;

	public SendSecretMessage(BigInteger secret, int senderId) {
		super();
		this.secret = secret;
		this.senderId = senderId;
	}

	public BigInteger getSecret() {
		return secret;
	}

	public void setSecret(BigInteger secret) {
		this.secret = secret;
	}

	public int getSenderId() {
		return senderId;
	}

	public void setSenderId(int senderId) {
		this.senderId = senderId;
	}
}
