package client.messages;

import java.io.Serializable;
import java.math.BigInteger;

public class ClientTimeStampRequest extends ClientRequest implements
		Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2205463457850363751L;

	public ClientTimeStampRequest(
			String dataId, long nonce) {
		super(dataId, nonce);
	}

}
