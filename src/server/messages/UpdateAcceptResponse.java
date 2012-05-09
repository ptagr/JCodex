package server.messages;

import java.io.Serializable;
import java.math.BigInteger;

import client.messages.CODEXClientMessage;

public class UpdateAcceptResponse extends InternalServerMessage implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4109941588742892050L;

	private CODEXClientMessage ccm;;
		

	public UpdateAcceptResponse(CODEXClientMessage ccm
			) {
		super();
		this.setCcm(ccm);
	}

	public UpdateAcceptResponse(CODEXClientMessage ccm,
			 int destId) {
		super(destId);
		this.setCcm(ccm);
	}
	




	public CODEXClientMessage getCcm() {
		return ccm;
	}



	public void setCcm(CODEXClientMessage ccm) {
		this.ccm = ccm;
	}
	
	
	
}
