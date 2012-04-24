package server.messages;

import java.io.Serializable;
import java.util.Set;

public class SignReadResponseRequest implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 3324393486009527281L;
	
	private Set<ForwardReadRequestAccept> evidenceSet;
	
	private BlindedReadResponse brr;

	public SignReadResponseRequest(Set<ForwardReadRequestAccept> evidenceSet,
			BlindedReadResponse brr) {
		super();
		this.evidenceSet = evidenceSet;
		this.brr = brr;
	}

	public Set<ForwardReadRequestAccept> getEvidenceSet() {
		return evidenceSet;
	}

	public void setEvidenceSet(Set<ForwardReadRequestAccept> evidenceSet) {
		this.evidenceSet = evidenceSet;
	}

	public BlindedReadResponse getBrr() {
		return brr;
	}

	public void setBrr(BlindedReadResponse brr) {
		this.brr = brr;
	}
	
	
	
}
