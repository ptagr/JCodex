package server.messages;

import java.io.Serializable;

import threshsig.SigShare;

public class SignedTimeStampResponse extends InternalServerMessage implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = -2008144844723943997L;

	private SigShare signedShare;

	private TimeStampReadResponse trr;
	
	public SignedTimeStampResponse(SigShare signedShare) {
		super();
		this.signedShare = signedShare;
	}
	
	public SignedTimeStampResponse(SigShare signedShare, TimeStampReadResponse trr) {
		super();
		this.signedShare = signedShare;
		this.trr = trr;
	}
	
	public SignedTimeStampResponse(SigShare signedShare, int destId) {
		super(destId);
		this.signedShare = signedShare;
	}
	
	public SignedTimeStampResponse(SigShare signedShare, TimeStampReadResponse trr , int destId) {
		super(destId);
		this.signedShare = signedShare;
		this.trr = trr;
	}

	public SigShare getSignedShare() {
		return signedShare;
	}

	public void setSignedShare(SigShare signedShare) {
		this.signedShare = signedShare;
	}

	public TimeStampReadResponse getTrr() {
		return trr;
	}

	public void setTrr(TimeStampReadResponse trr) {
		this.trr = trr;
	}
	
	
	
	
	
}
