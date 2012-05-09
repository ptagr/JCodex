package client.messages;

import java.io.Serializable;
import java.math.BigInteger;

import server.messages.CODEXServerMessage;
import server.messages.TimeStampReadResponse;

public class ClientUpdateRequest extends ClientRequest implements Serializable {
	

	/**
	 * 
	 */
	private static final long serialVersionUID = -685220797403035429L;
	private BigInteger encryptedSecret;
	private CODEXClientMessage ccm;
	
	public ClientUpdateRequest(BigInteger encryptedSecret,String dataId, long nonce, CODEXClientMessage ccm) {
		super(dataId, nonce);
		this.encryptedSecret = encryptedSecret;
		this.setCcm(ccm);
	}
	

	public BigInteger getEncryptedSecret() {
		return encryptedSecret;
	}

	public void setEncryptedSecret(BigInteger encryptedSecret) {
		this.encryptedSecret = encryptedSecret;
	}


	public CODEXClientMessage getCcm() {
		return ccm;
	}


	public void setCcm(CODEXClientMessage ccm) {
		this.ccm = ccm;
	}


}
