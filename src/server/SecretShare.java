package server;

import java.io.Serializable;
import java.math.BigInteger;

public class SecretShare implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 5303019265149257240L;
	private BigInteger timestamp;
	private BigInteger secret;

	public SecretShare(BigInteger timestamp, BigInteger secret) {
		super();
		this.timestamp = timestamp;
		this.secret = secret;
	}

	public BigInteger getSecret() {
		return secret;
	}

	public void setSecret(BigInteger secret) {
		this.secret = secret;
	}

	public BigInteger getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(BigInteger timestamp) {
		this.timestamp = timestamp;
	}

	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return timestamp.toString() + "-" + secret.toString();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if ((obj == null) || (obj.getClass() != this.getClass()))
			return false;

		SecretShare ss = (SecretShare) obj;
		return timestamp.equals(ss.getTimestamp())
				&& secret.equals(ss.getSecret());
	}
	
	@Override
	public int hashCode() {
		// TODO Auto-generated method stub
		int hash = 7;
		hash = 31 * hash + this.getSecret().hashCode();
		hash = 31 * hash + this.getTimestamp().hashCode();
		return hash;
	}
}
