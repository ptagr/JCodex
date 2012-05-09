package server.messages;

import java.io.Serializable;
import java.math.BigInteger;

import threshsig.SigShare;

public class ForwardReadRequestAccept implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 4109941588742892050L;

	private BigInteger digitalSig;
	
	private BigInteger cipher;
	
	private SigShare decryptedShare;
	
	private Integer timeStamp;

	public ForwardReadRequestAccept(BigInteger digitalSig, BigInteger cipher,
			SigShare decryptedShare, Integer timestamp) {
		super();
		this.setDigitalSig(digitalSig);
		this.setCipher(cipher);
		this.setDecryptedShare(decryptedShare);
		this.setTimeStamp(timestamp);
	}

	public BigInteger getDigitalSig() {
		return digitalSig;
	}

	public void setDigitalSig(BigInteger digitalSig) {
		this.digitalSig = digitalSig;
	}

	public BigInteger getCipher() {
		return cipher;
	}

	public void setCipher(BigInteger cipher) {
		this.cipher = cipher;
	}

	public SigShare getDecryptedShare() {
		return decryptedShare;
	}

	public void setDecryptedShare(SigShare decryptedShare) {
		this.decryptedShare = decryptedShare;
	}

	public Integer getTimeStamp() {
		return timeStamp;
	}

	public void setTimeStamp(Integer timeStamp) {
		this.timeStamp = timeStamp;
	}
	
	
	
}
