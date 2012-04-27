package server.messages;

import java.io.Serializable;
import java.math.BigInteger;

public class TimeStampResponse implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 4109941588742892050L;

	private BigInteger digitalSig;
		
	private BigInteger timeStamp;

	public TimeStampResponse(BigInteger digitalSig,
			BigInteger timestamp) {
		super();
		this.setDigitalSig(digitalSig);
		this.setTimeStamp(timestamp);
	}

	public BigInteger getDigitalSig() {
		return digitalSig;
	}

	public void setDigitalSig(BigInteger digitalSig) {
		this.digitalSig = digitalSig;
	}

	public BigInteger getTimeStamp() {
		return timeStamp;
	}

	public void setTimeStamp(BigInteger timeStamp) {
		this.timeStamp = timeStamp;
	}
	
	
	
}
