package server;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import client.messages.CODEXClientMessage;

import junit.framework.Assert;

import main.Constants;
import server.messages.CODEXServerMessage;
import threshsig.SigShare;
import utils.KeyUtility;
import utils.SerializationUtil;

public class ServerKeyManager implements Runnable {

	private HashMap<Integer, PublicKey> clientPublicKeys = new HashMap<Integer, PublicKey>();
	private HashMap<Integer, Signature> clientSignatureEngines = new HashMap<Integer, Signature>();
	private HashMap<Integer, PublicKey> serverPublicKeys = new HashMap<Integer, PublicKey>();
	private HashMap<Integer, Signature> serverSignatureEngines = new HashMap<Integer, Signature>();

	private int serverId;
	private Signature signatureEngine;
	private PrivateKey privateKey;

	public ServerKeyManager(Set<Integer> clientIds, int serverId, int servers) {
		this.serverId = serverId;

		Set<Integer> serverIds = new HashSet<Integer>();
		for (int i = 0; i < servers; i++) {
			// if (i != serverId) {
			serverIds.add(i);
			// }
		}

		initKey(clientIds, serverIds);
		initKey();
	}

	public void initKey() {

		try {

			this.privateKey = KeyUtility.getPrivateKey(Constants.CONFIG_DIR
					+ "/" + Constants.SERVER_PRIVATE_KEY_FILE + this.serverId);

			signatureEngine = Signature.getInstance("SHA1withRSA");
			signatureEngine.initSign(this.privateKey, new SecureRandom());

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
	public void initKey(Set<Integer> clientIds, Set<Integer> serverIds) {

		// Populate the client public keys
		for (Integer clientId : clientIds) {
			try {

				PublicKey publicKey = KeyUtility
						.getPublicKey(Constants.CONFIG_DIR + "/"
								+ Constants.CLIENT_PUBLIC_KEY_FILE + clientId);

				clientPublicKeys.put(clientId, publicKey);

				Signature signatureEngine = Signature
						.getInstance("SHA1withRSA");
				signatureEngine.initVerify(publicKey);
				clientSignatureEngines.put(clientId, signatureEngine);

			} catch (NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InvalidKeyException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		// Populate the server public keys
		for (Integer serverId : serverIds) {
			try {
				PublicKey publicKey = KeyUtility
						.getPublicKey(Constants.CONFIG_DIR + "/"
								+ Constants.SERVER_PUBLIC_KEY_FILE + serverId);

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

	public boolean verifyClientSignature(int clientId, byte[] signedData,
			byte[] signature) {
		Signature signEngine = clientSignatureEngines.get(clientId);
		if (signEngine == null)
			return false;

		try {
			signEngine.update(signedData);
			return signEngine.verify(signature);
		} catch (SignatureException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;

	}

	public boolean verifyClientSignature(CODEXClientMessage cm) {
		Signature signEngine = clientSignatureEngines.get(cm.getSenderId());

		if (signEngine == null)
			return false;
		try {
			signEngine.update(cm.getSerializedMessage());
			return signEngine.verify(cm.getSerializedMessageSignature());
		} catch (SignatureException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;

	}

	public boolean verifyServerSignature(CODEXServerMessage sm) {
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

	public void signMessage(CODEXServerMessage csm) {
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

	@Override
	public void run() {

	}

}
