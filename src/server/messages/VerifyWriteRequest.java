package server.messages;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.Set;

import client.messages.ClientWriteRequest;

public class VerifyWriteRequest implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 5455562565865309371L;
	private ClientWriteRequest cwr;
	private WriteSecretResponse wsr;
	private BigInteger timestamp;
	private Set<ForwardWriteRequestAccept> evidenceSet;

	public VerifyWriteRequest(ClientWriteRequest crw,
			WriteSecretResponse wsr,
			Set<ForwardWriteRequestAccept> evidenceSet,
			BigInteger timestamp) {
		super();
		this.cwr = crw;
		this.setWsr(wsr);
		this.evidenceSet = evidenceSet;
		this.timestamp = timestamp;
	}

	public ClientWriteRequest getCwr() {
		return cwr;
	}

	public void setCwr(ClientWriteRequest crw) {
		this.cwr = crw;
	}



	public Set<ForwardWriteRequestAccept> getEvidenceSet() {
		return evidenceSet;
	}

	public void setEvidenceSet(Set<ForwardWriteRequestAccept> evidenceSet) {
		this.evidenceSet = evidenceSet;
	}

	public WriteSecretResponse getWsr() {
		return wsr;
	}

	public void setWsr(WriteSecretResponse wsr) {
		this.wsr = wsr;
	}

	public BigInteger getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(BigInteger timestamp) {
		this.timestamp = timestamp;
	}
	
	

}
