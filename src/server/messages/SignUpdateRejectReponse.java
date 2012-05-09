package server.messages;

import java.io.Serializable;
import java.util.Set;

public class SignUpdateRejectReponse extends InternalServerMessage implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3324393486009527281L;

	private Set<CODEXServerMessage> evidenceSet;

	private ClientUpdateRejectResponse wrr;

	public SignUpdateRejectReponse(Set<CODEXServerMessage> evidenceSet,
			ClientUpdateRejectResponse wrr) {
		super();
		this.evidenceSet = evidenceSet;
		this.setWrr(wrr);
	}
	
	public SignUpdateRejectReponse(Set<CODEXServerMessage> evidenceSet,
			ClientUpdateRejectResponse wrr, int destId) {
		super(destId);
		this.evidenceSet = evidenceSet;
		this.setWrr(wrr);
	}

	public Set<CODEXServerMessage> getEvidenceSet() {
		return evidenceSet;
	}

	public void setEvidenceSet(Set<CODEXServerMessage> evidenceSet) {
		this.evidenceSet = evidenceSet;
	}

	public ClientUpdateRejectResponse getWrr() {
		return wrr;
	}

	public void setWrr(ClientUpdateRejectResponse wrr) {
		this.wrr = wrr;
	}

	

}
