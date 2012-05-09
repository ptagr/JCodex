package server.messages;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.Set;

import client.messages.CODEXClientMessage;
import client.messages.ClientUpdateRequest;

public class UpdateRequest extends InternalServerMessage implements
		Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 5455562565865309371L;
	private CODEXClientMessage ccm;

	public UpdateRequest(CODEXClientMessage ccm) {
		super();
		this.setCcm(ccm);
	}

	public UpdateRequest(CODEXClientMessage ccm, int destId) {
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
