package server.messages;

import java.io.Serializable;

public class InternalServerMessage implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 2008572976486061633L;
	private int destinationId;
	
	
	
	public InternalServerMessage() {
		super();
	}

	public InternalServerMessage(int destinationId) {
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
