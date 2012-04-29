package server.messages;

import java.io.Serializable;
import java.math.BigInteger;

import client.messages.CODEXClientMessage;

public class TimeStampResponse extends InternalServerMessage implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4109941588742892050L;

	private CODEXClientMessage ccm;;
		
	private BigInteger timeStamp;

	public TimeStampResponse(CODEXClientMessage ccm,
			BigInteger timestamp) {
		super();
		this.setCcm(ccm);
		this.setTimeStamp(timestamp);
	}

	public TimeStampResponse(CODEXClientMessage ccm,
			BigInteger timestamp, int destId) {
		super(destId);
		this.setCcm(ccm);
		this.setTimeStamp(timestamp);
	}
	

	public BigInteger getTimeStamp() {
		return timeStamp;
	}

	public void setTimeStamp(BigInteger timeStamp) {
		this.timeStamp = timeStamp;
	}



	public CODEXClientMessage getCcm() {
		return ccm;
	}



	public void setCcm(CODEXClientMessage ccm) {
		this.ccm = ccm;
	}
	
	
	
}
