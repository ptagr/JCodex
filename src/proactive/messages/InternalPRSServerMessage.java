package proactive.messages;

import java.io.Serializable;

public class InternalPRSServerMessage implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 5484748547774274932L;
	private int destinationId;

	public InternalPRSServerMessage() {
		super();
	}

	public InternalPRSServerMessage(int destinationId) {
		super();
		this.destinationId = destinationId;
	}

	public int getDestinationId() {
		return destinationId;
	}

	public void setDestinationId(int destinationId) {
		this.destinationId = destinationId;
	}
}
