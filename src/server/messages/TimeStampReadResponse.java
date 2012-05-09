package server.messages;

import java.io.Serializable;
import java.math.BigInteger;

import client.messages.CODEXClientMessage;
import client.messages.ClientReadRequest;
import client.messages.ClientTimeStampRequest;

public class TimeStampReadResponse implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 7664350108995209314L;

	private CODEXClientMessage ccm;
	private Integer timestamp;

	private String dataId;

	
	public TimeStampReadResponse(CODEXClientMessage ccm,
			Integer timestamp, String dataId) {
		super();
		this.setCcm(ccm);
		this.timestamp = timestamp;
		this.dataId = dataId;
	}

	public String getDataId() {
		return dataId;
	}

	public void setDataId(String dataId) {
		this.dataId = dataId;
	}

	public Integer getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Integer timestamp) {
		this.timestamp = timestamp;
	}

	public CODEXClientMessage getCcm() {
		return ccm;
	}

	public void setCcm(CODEXClientMessage ccm) {
		this.ccm = ccm;
	}

	
}
