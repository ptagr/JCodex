package server.messages;

import java.io.Serializable;

import threshsig.SigShare;

public class SignedReadResponse implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = -2008144844723943997L;

	private SigShare signedShare;

	
	
	public SignedReadResponse(SigShare signedShare) {
		super();
		this.signedShare = signedShare;
	}

	public SigShare getSignedShare() {
		return signedShare;
	}

	public void setSignedShare(SigShare signedShare) {
		this.signedShare = signedShare;
	}
	
	
	
	
	
}
