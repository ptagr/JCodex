package server;

import java.math.BigInteger;

public class SecretShare {
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
		return  timestamp.toString() +"-"+secret.toString();
	}
	
}
