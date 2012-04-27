package server.messages;

public enum CODEXServerMessageType {
	TIMESTAMP_REQUEST,
	TIMESTAMP_RESPONSE,
	TIMESTAMP_REQUEST_REJECT,
	
	FORWARD_READ_REQUEST,
	FORWARD_READ_REQUEST_REJECT,
	FORWARD_READ_REQUEST_ACCEPT,
	BLINDED_READ_RESPONSE,
	SIGN_READ_RESPONSE_REQUEST,
	SIGNED_READ_RESPONSE,
	FORWARD_WRITE_REQUEST,
	FORWARD_WRITE_REQUEST_REJECT,
	FORWARD_WRITE_REQUEST_ACCEPT,
	VERIFY_WRITE_REQUEST,
	VERIFIED_WRITE_REQUEST,
	SIGN_WRITE_RESPONSE_REQUEST,
	SIGNED_WRITE_RESPONSE
}
