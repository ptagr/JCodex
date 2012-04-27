package server.messages;

import java.io.Serializable;

import client.messages.CODEXClientMessage;
import client.messages.ClientWriteRequest;

public class TimeStampRequest implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = -1756443845337551106L;
	
	private CODEXClientMessage ccm;

	public TimeStampRequest(CODEXClientMessage ccm) {
		super();
		this.ccm = ccm;
	}

	public CODEXClientMessage getCcm() {
		return ccm;
	}

	public void setCwr(CODEXClientMessage ccm) {
		this.ccm = ccm;
	}

}
