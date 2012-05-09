package server.messages;

import java.io.Serializable;
import java.math.BigInteger;

import client.messages.CODEXClientMessage;
import client.messages.ClientReadRequest;
import client.messages.ClientUpdateRequest;

public class ClientUpdateAcceptResponse implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 7301595879569959674L;

	private CODEXClientMessage ccm;

	private String dataId;
	

	public ClientUpdateAcceptResponse(CODEXClientMessage ccm, String dataId) {
		super();
		this.setCcm(ccm);
		this.dataId = dataId;
	}

	public String getDataId() {
		return dataId;
	}

	public void setDataId(String dataId) {
		this.dataId = dataId;
	}

	public CODEXClientMessage getCcm() {
		return ccm;
	}

	public void setCcm(CODEXClientMessage ccm) {
		this.ccm = ccm;
	}

	
}
