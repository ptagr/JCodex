package server.messages;

import java.io.Serializable;
import java.math.BigInteger;

import client.messages.ClientReadRequest;
import client.messages.ClientWriteRequest;

public class WriteSecretResponse implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 7301595879569959674L;

	private ClientWriteRequest crw;

	private String dataId;
	

	public WriteSecretResponse(ClientWriteRequest crw, String dataId) {
		super();
		this.crw = crw;
		this.dataId = dataId;
	}

	public String getDataId() {
		return dataId;
	}

	public void setDataId(String dataId) {
		this.dataId = dataId;
	}

	public ClientWriteRequest getCrw() {
		return crw;
	}

	public void setCrw(ClientWriteRequest crw) {
		this.crw = crw;
	}
}
