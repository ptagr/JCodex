package proactive;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import main.Constants;
import proactive.messages.PRSMessage;
import proactive.messages.PRSServerMessage;
import utils.KeyUtility;
import utils.SerializationUtil;

public class PRSServerKeyManager {

	private HashMap<Integer, PublicKey> serverPublicKeys = new HashMap<Integer, PublicKey>();
	private HashMap<Integer, Signature> serverSignatureEngines = new HashMap<Integer, Signature>();

	private PublicKey payloadPublicKey;
	private Signature payloadSignatureEngine;

	private int serverId;
	private Signature signatureEngine;
	private PrivateKey privateKey;

	public PRSServerKeyManager(int serverId, int servers) {
		this.serverId = serverId;

		Set<Integer> serverIds = new HashSet<Integer>();
		for (int i = 0; i < servers; i++) {
			// if (i != serverId) {
			serverIds.add(i);
			// }
		}

		initKey(serverIds);
		initKey();
	}

	public void initKey() {

		try {

			this.privateKey = KeyUtility
					.getPrivateKey(Constants.THRESHOLD_PRS_CONFIG_DIR
							+ serverId + "/" + Constants.PRS_PRIVATE_KEY_FILE
							+ this.serverId);

			signatureEngine = Signature.getInstance("SHA1withRSA");
			signatureEngine.initSign(this.privateKey, new SecureRandom());

			// Initialize the PRS signatureEngine
			this.payloadPublicKey = KeyUtility
					.getPublicKey(Constants.CONFIG_DIR + serverId + "/"
							+ Constants.SERVER_PUBLIC_KEY_FILE + this.serverId);

			payloadSignatureEngine = Signature.getInstance("SHA1withRSA");
			payloadSignatureEngine.initVerify(this.payloadPublicKey);

		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	/*
	 * Read the client and server keys from config dir
	 */
	public void initKey(Set<Integer> serverIds) {

		// Populate the server public keys
		for (Integer serverId : serverIds) {
			try {
				PublicKey publicKey = KeyUtility
						.getPublicKey(Constants.CONFIG_DIR + serverId + "/"
								+ Constants.PRS_PUBLIC_KEY_FILE + serverId);

				serverPublicKeys.put(serverId, publicKey);

				Signature signatureEngine = Signature
						.getInstance("SHA1withRSA");
				signatureEngine.initVerify(publicKey);
				serverSignatureEngines.put(serverId, signatureEngine);
			} catch (NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InvalidKeyException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

	public boolean verifyServerSignature(PRSServerMessage sm) {
		Signature signEngine = serverSignatureEngines.get(sm.getSenderId());

		if (signEngine == null) {
			System.out.println("SignEngine null");
			return false;
		} else {
			try {
				signEngine.update(sm.getSerializedMessage());
				return signEngine.verify(sm.getSerializedMessageSignature());
			} catch (SignatureException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
		return false;
	}

	public boolean verifyPRSSignature(PRSMessage sm) {

		if (payloadSignatureEngine == null) {
			System.out.println("PRSSignEngine null");
			return false;
		} else {
			try {
				payloadSignatureEngine.update(sm.getSerializedMessage());
				return payloadSignatureEngine.verify(sm
						.getSerializedMessageSignature());
			} catch (SignatureException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
		return false;
	}

	public boolean verifyServerSignature(byte[] data, byte[] sig, int sId) {
		Signature signEngine = serverSignatureEngines.get(sId);

		if (signEngine == null)
			return false;
		try {
			signEngine.update(data);
			return signEngine.verify(sig);
		} catch (SignatureException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;

	}

	public void signMessage(PRSServerMessage csm) {
		try {
			// System.out.println(csm.getSerializedMessage());

			signatureEngine.update(csm.getSerializedMessage());
			csm.setSerializedMessageSignature(signatureEngine.sign());
		} catch (SignatureException e) {
			e.printStackTrace();

		}
		return;

	}

	public void signMessage(PRSMessage csm) {
		try {
			// System.out.println(csm.getSerializedMessage());

			signatureEngine.update(csm.getSerializedMessage());
			csm.setSerializedMessageSignature(signatureEngine.sign());
		} catch (SignatureException e) {
			e.printStackTrace();

		}
		return;

	}

	public byte[] getSignature(Object o) {
		try {
			signatureEngine.update(SerializationUtil.serialize(o));
			return signatureEngine.sign();
		} catch (SignatureException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();

		}
		return null;

	}

}
