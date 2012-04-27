package server.messages;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.Set;

import client.messages.CODEXClientMessage;
import client.messages.ClientWriteRequest;

public class ForwardWriteRequest implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 5455562565865309371L;
	private CODEXClientMessage cwr;
	private BigInteger timestamp;
	private Set<TimeStampResponse> evidenceSet;

	public ForwardWriteRequest(CODEXClientMessage crw,
			BigInteger timestamp,
			Set<TimeStampResponse> evidenceSet) {
		super();
		this.cwr = crw;
		this.setTimestamp(timestamp);
		this.evidenceSet = evidenceSet;
	}

	public CODEXClientMessage getCwr() {
		return cwr;
	}

	public void setCwr(CODEXClientMessage crw) {
		this.cwr = crw;
	}



	public Set<TimeStampResponse> getEvidenceSet() {
		return evidenceSet;
	}

	public void setEvidenceSet(Set<TimeStampResponse> evidenceSet) {
		this.evidenceSet = evidenceSet;
	}

	public BigInteger getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(BigInteger timestamp) {
		this.timestamp = timestamp;
	}

	

}
