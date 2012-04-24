package server.messages;

import java.io.Serializable;
import java.util.Set;

public class SignWriteResponseRequest implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3324393486009527281L;

	private Set<VerifiedWriteRequest> evidenceSet;

	private WriteSecretResponse wsr;

	public SignWriteResponseRequest(Set<VerifiedWriteRequest> evidenceSet,
			WriteSecretResponse wsr) {
		super();
		this.evidenceSet = evidenceSet;
		this.wsr = wsr;
	}

	public Set<VerifiedWriteRequest> getEvidenceSet() {
		return evidenceSet;
	}

	public void setEvidenceSet(Set<VerifiedWriteRequest> evidenceSet) {
		this.evidenceSet = evidenceSet;
	}

	public WriteSecretResponse getWsr() {
		return wsr;
	}

	public void setWsr(WriteSecretResponse wsr) {
		this.wsr = wsr;
	}

}
