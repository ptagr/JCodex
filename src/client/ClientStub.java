package client;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;
import java.util.Random;

import client.messages.CODEXClientMessage;
import client.messages.CODEXClientMessageType;
import client.messages.ClientReadRequest;
import client.messages.ClientWriteRequest;

import main.Constants;
import server.messages.BlindedReadResponse;
import server.messages.WriteSecretResponse;
import threshsig.GroupKey;
import utils.ClientTimeUtility;
import utils.KeyUtility;
import utils.SerializationUtil;

public class ClientStub {
	private PrivateKey privateKey;
	// private int keySize = 1024;
	private int clientId;
	Signature signatureEngine;
	ClientConnectionManager ccm;
	private GroupKey gk;

	// public static int k = 2;
	// public static int l = 4;
	// public static int baseServerPort = 9000;
	public static int baseClientPort = 9000;

	public ClientStub(int l, int k, int baseServerPort, int clientId) {
		this.ccm = new ClientConnectionManager(k, l, baseClientPort + clientId,
				baseServerPort);
		// this.ccm.establishLocalConnections();
		// this.k = k;
		// this.l = l;

		this.clientId = clientId;
		try {
			initKey();
		} catch (InvalidKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	/*
	 * Read the private key for the client from config dir and initialize
	 * signature Engine
	 */
	public void initKey() throws NoSuchAlgorithmException, InvalidKeyException {
		if (privateKey == null) {

			this.privateKey = KeyUtility.getPrivateKey(Constants.CONFIG_DIR
					+ "/" + Constants.CLIENT_PRIVATE_KEY_FILE + this.clientId);

		}
		signatureEngine = Signature.getInstance("SHA1withRSA");
		signatureEngine.initSign(privateKey, new SecureRandom());

		try {
			ObjectInputStream ois = new ObjectInputStream(new FileInputStream(
					Constants.THRESHOLD_CONFIG_DIR + "/"
							+ Constants.THRESHOLD_GROUP_KEY_FILE));
			gk = (GroupKey) ois.readObject();

			ois.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public BigInteger getSecret(String dataId) {

		ClientTimeUtility ctu = new ClientTimeUtility();
		long nonce = new Random().nextInt();
		BigInteger bp = new BigInteger("blindingfactor".getBytes());

		// Encode the bp using CODEX public key
		BigInteger enc = bp.modPow(gk.getExponent(), gk.getModulus());

		// System.out.println(enc);
		ClientReadRequest crr = new ClientReadRequest(enc, dataId, nonce);

		byte data[] = SerializationUtil.serialize(crr);

		CODEXClientMessage cm = new CODEXClientMessage(nonce, 0,
				CODEXClientMessageType.CLIENT_READ_SECRET_REQUEST, data);
		// sendMessage(cm, true);

		if (cm.getSerializedMessage() == null) {
			return null;
		}

		cm.setSenderId(this.getClientId());

		// produce signature
		// if (sign) {
		cm.setSerializedMessageSignature(signMessage(cm.getSerializedMessage()));
		// }

		byte[] dataToSend = SerializationUtil.serialize(cm);

		// Send data over the channel to the delegate
		ccm.sendMessage(dataToSend);

		while (true && ctu.timerHasNotExpired()) {
			CODEXClientMessage cmm;
			try {
				cmm = ccm.getNextMessageSynchronous();
				if (cmm.getType().equals(
						CODEXClientMessageType.BLINDED_READ_RESPONSE)) {

					// Verify CODEX signature
					if (verifySignature(cmm)) {

						// Verify the client message to check if its not a
						// replay
						BlindedReadResponse brr = (BlindedReadResponse) SerializationUtil
								.deserialize(cmm.getSerializedMessage());
						ClientReadRequest crr2 = brr.getCrr();
						if (crr.getNonce() == crr2.getNonce()) {

							BigInteger bs = brr.getBlindedSecret();

							BigInteger originalSecret = bs.divide(bp);
							// BigInteger originalSecret = bs;

							// System.out.println("Original secret : "
							// + originalSecret.toString());
							System.out.println("Retrieved secret for dataId "+ brr.getDataId()+": "
									+ new String(originalSecret.toByteArray()));
							
							return originalSecret;
							
							
						} else {
							System.out.println("Cannot verify nonce");
						}
					} else {
						System.out.println("Cannot verify signature");
					}
				} else {
					// Ignore
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

		return null;
	}
	
	
	public boolean setSecret(String dataId, String secret) {

		ClientTimeUtility ctu = new ClientTimeUtility();
		long nonce = new Random().nextInt();
		BigInteger bp = new BigInteger(secret.getBytes());

		// Encode the bp using CODEX public key
		BigInteger enc = bp.modPow(gk.getExponent(), gk.getModulus());

		// System.out.println(enc);
		ClientWriteRequest cwr = new ClientWriteRequest(enc, dataId, nonce);

		byte data[] = SerializationUtil.serialize(cwr);

		CODEXClientMessage cm = new CODEXClientMessage(nonce, 0,
				CODEXClientMessageType.CLIENT_WRITE_SECRET_REQUEST, data);
		// sendMessage(cm, true);

		if (cm.getSerializedMessage() == null) {
			return false;
		}

		cm.setSenderId(this.getClientId());

		// produce signature
		// if (sign) {
		cm.setSerializedMessageSignature(signMessage(cm.getSerializedMessage()));
		// }

		byte[] dataToSend = SerializationUtil.serialize(cm);

		// Send data over the channel to the delegate
		ccm.sendMessage(dataToSend);

		while (true && ctu.timerHasNotExpired()) {
			CODEXClientMessage cmm;
			try {
				cmm = ccm.getNextMessageSynchronous();
				if (cmm.getType().equals(
						CODEXClientMessageType.WRITE_SECRET_REPONSE)) {

					// Verify CODEX signature
					if (verifySignature(cmm)) {

						// Verify the client message to check if its not a
						// replay
						WriteSecretResponse wsr = (WriteSecretResponse) SerializationUtil
								.deserialize(cmm.getSerializedMessage());
						ClientWriteRequest cwr2 = wsr.getCrw();
						if (cwr.getNonce() == cwr2.getNonce()) {

							String secretId = wsr.getDataId();
							
							System.out.println("Succesfully stored secret for dataId "+ secretId);
//							BigInteger bs = brr.getBlindedSecret();
//
//							BigInteger originalSecret = bs.divide(bp);
//							// BigInteger originalSecret = bs;
//
//							// System.out.println("Original secret : "
//							// + originalSecret.toString());
//							System.out.println("Retrieved secret : "
//									+ new String(originalSecret.toByteArray()));
							
							
							return true;
						} else {
							System.out.println("Cannot verify nonce");
						}
					} else {
						System.out.println("Cannot verify signature");
					}
				} else {
					// Ignore
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

		return false;
	}

	public boolean verifySignature(CODEXClientMessage ccm) {
		byte[] message = ccm.getSerializedMessage();
		MessageDigest mDigest;
		try {
			mDigest = MessageDigest.getInstance("SHA");
			mDigest.update(message);
			byte[] bi = mDigest.digest();

			BigInteger recdSign = new BigInteger(
					ccm.getSerializedMessageSignature());
			BigInteger recdHash = recdSign.modPow(gk.getExponent(),
					gk.getModulus());

			BigInteger hash = new BigInteger(1, bi);
			//System.out.println("Data to verify : " + new BigInteger(1, bi));

			return hash.equals(recdHash);
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return false;
	}

	public byte[] signMessage(byte[] data) {
		try {
			// MessageDigest md = MessageDigest.getInstance("SHA1");
			//
			// // 3. Create a Message Digest of the Data to be transmitted
			// md.update(data);
			// byte byteMDofDataToTransmit[] = md.digest();
			//
			// String strMDofDataToTransmit = new String();
			// for (int i = 0; i < byteMDofDataToTransmit.length; i++) {
			// strMDofDataToTransmit = strMDofDataToTransmit
			// + Integer
			// .toHexString((int) byteMDofDataToTransmit[i] & 0xFF);
			// }

			signatureEngine.update(data);
			return signatureEngine.sign();
		} catch (SignatureException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();

		}
		return null;

	}

	public int getClientId() {
		return clientId;
	}

	public void setClientId(int clientId) {
		this.clientId = clientId;
	}

}
