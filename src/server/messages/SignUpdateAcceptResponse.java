package server.messages;

import java.io.Serializable;
import java.util.Set;

public class SignUpdateAcceptResponse extends InternalServerMessage implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3324393486009527281L;

	private Set<CODEXServerMessage> evidenceSet;

	private ClientUpdateAcceptResponse wsr;

	public SignUpdateAcceptResponse(Set<CODEXServerMessage> evidenceSet,
			ClientUpdateAcceptResponse wsr) {
		super();
		this.evidenceSet = evidenceSet;
		this.wsr = wsr;
	}
	
	public SignUpdateAcceptResponse(Set<CODEXServerMessage> evidenceSet,
			ClientUpdateAcceptResponse wsr, int destId) {
		super(destId);
		this.evidenceSet = evidenceSet;
		this.wsr = wsr;
	}

	public Set<CODEXServerMessage> getEvidenceSet() {
		return evidenceSet;
	}

	public void setEvidenceSet(Set<CODEXServerMessage> evidenceSet) {
		this.evidenceSet = evidenceSet;
	}

	public ClientUpdateAcceptResponse getWsr() {
		return wsr;
	}

	public void setWsr(ClientUpdateAcceptResponse wsr) {
		this.wsr = wsr;
	}

}
