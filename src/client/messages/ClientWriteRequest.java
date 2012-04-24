package client.messages;

import java.io.Serializable;
import java.math.BigInteger;

public class ClientWriteRequest extends ClientRequest implements Serializable {
	

	/**
	 * 
	 */
	private static final long serialVersionUID = -685220797403035429L;
	private BigInteger encryptedSecret;
	
	public ClientWriteRequest(BigInteger encryptedSecret,String dataId, long nonce) {
		super(dataId, nonce);
		this.encryptedSecret = encryptedSecret;
	}
	

	public BigInteger getEncryptedSecret() {
		return encryptedSecret;
	}

	public void setEncryptedSecret(BigInteger encryptedSecret) {
		this.encryptedSecret = encryptedSecret;
	}

}
