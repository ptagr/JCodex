package server.messages;

import java.io.Serializable;
import java.util.Set;

public class SignTimeStampResponseRequest extends InternalServerMessage implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 3324393486009527281L;
	
	private Set<CODEXServerMessage> evidenceSet;
	
	private TimeStampReadResponse trr;

	public SignTimeStampResponseRequest(Set<CODEXServerMessage> evidenceSet,
			TimeStampReadResponse trr) {
		super();
		this.evidenceSet = evidenceSet;
		this.setTrr(trr);
	}
	
	public SignTimeStampResponseRequest(Set<CODEXServerMessage> evidenceSet,
			TimeStampReadResponse trr, int destId) {
		super(destId);
		this.evidenceSet = evidenceSet;
		this.setTrr(trr);
	}

	public Set<CODEXServerMessage> getEvidenceSet() {
		return evidenceSet;
	}

	public void setEvidenceSet(Set<CODEXServerMessage> evidenceSet) {
		this.evidenceSet = evidenceSet;
	}

	public TimeStampReadResponse getTrr() {
		return trr;
	}

	public void setTrr(TimeStampReadResponse trr) {
		this.trr = trr;
	}

	
	
	
	
}
