package server.messages;

import java.io.Serializable;
import java.math.BigInteger;

import client.messages.CODEXClientMessage;
import client.messages.ClientUpdateRequest;

public class TimeStampRequest extends InternalServerMessage implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = -1756443845337551106L;
	
	private CODEXClientMessage ccm;

	public TimeStampRequest(CODEXClientMessage ccm) {
		super();
		this.ccm = ccm;
	}

	
	
	public TimeStampRequest(CODEXClientMessage ccm, int destId) {
		super(destId);
		this.ccm = ccm;
	}



	public CODEXClientMessage getCcm() {
		return ccm;
	}

	public void setCwr(CODEXClientMessage ccm) {
		this.ccm = ccm;
	}

	

	

}
