package client.messages;

import java.io.Serializable;
import java.math.BigInteger;

public class ClientReadRequest extends ClientRequest implements Serializable{
	
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -2205463457850363751L;
	private BigInteger encryptedBlindingFactor;
	
	

	public ClientReadRequest(BigInteger encryptedBlindingFactor, String dataId, long nonce) {
		super(dataId, nonce);
		this.encryptedBlindingFactor = encryptedBlindingFactor;
	}

	public BigInteger getEncryptedBlindingFactor() {
		return encryptedBlindingFactor;
	}

	public void setEncryptedBlindingFactor(BigInteger encryptedBlindingFactor) {
		this.encryptedBlindingFactor = encryptedBlindingFactor;
	}

	
	
}
