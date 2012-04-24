package server.messages;

import java.io.Serializable;
import java.math.BigInteger;

import client.messages.ClientReadRequest;

public class BlindedReadResponse implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 7664350108995209314L;

	private ClientReadRequest crr;

	private BigInteger blindedSecret;

	private String dataId;

	public BlindedReadResponse(ClientReadRequest crr, BigInteger blindedSecret,
			String dataId) {
		super();
		this.setCrr(crr);
		this.setBlindedSecret(blindedSecret);
		this.setDataId(dataId);
	}

	public ClientReadRequest getCrr() {
		return crr;
	}

	public void setCrr(ClientReadRequest crr) {
		this.crr = crr;
	}

	public BigInteger getBlindedSecret() {
		return blindedSecret;
	}

	public void setBlindedSecret(BigInteger blindedSecret) {
		this.blindedSecret = blindedSecret;
	}

	public String getDataId() {
		return dataId;
	}

	public void setDataId(String dataId) {
		this.dataId = dataId;
	}
}
