package proactive.messages;

import java.io.Serializable;
import java.util.HashMap;

import server.SecretShare;

public class SendStateMessage extends InternalPRSServerMessage implements
		Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1919515196706588689L;

	// State of the sending server
	private HashMap<String, SecretShare> shareDB;

	private int senderId;

	public SendStateMessage(HashMap<String, SecretShare> shareDB, int senderId) {
		super();
		this.shareDB = shareDB;
		this.senderId = senderId;
	}

	public HashMap<String, SecretShare> getShareDB() {
		return shareDB;
	}

	public void setShareDB(HashMap<String, SecretShare> shareDB) {
		this.shareDB = shareDB;
	}

	public int getSenderId() {
		return senderId;
	}

	public void setSenderId(int senderId) {
		this.senderId = senderId;
	}
}
