package client.messages;

import java.io.Serializable;

public class ClientRequest implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 5765468296684965697L;
	private String dataId;
	private long nonce; // Nonce number defined by the client

	
	
	public ClientRequest(String dataId, long nonoce) {
		super();
		this.dataId = dataId;
		this.setNonce(nonoce);
	}

	public String getDataId() {
		return dataId;
	}

	public void setDataId(String dataId) {
		this.dataId = dataId;
	}

	public long getNonce() {
		return nonce;
	}

	public void setNonce(long nonce) {
		this.nonce = nonce;
	}
}
