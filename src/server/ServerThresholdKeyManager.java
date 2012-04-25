package server;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Set;

import main.Constants;
import server.messages.ForwardReadRequestAccept;
import threshsig.GroupKey;
import threshsig.KeyShare;
import threshsig.SigShare;
import utils.SerializationUtil;

public class ServerThresholdKeyManager {
	private static GroupKey gk;
	private static KeyShare keyshare;
	private int serverId;

	ServerThresholdKeyManager(int serverId) {
		this.serverId = serverId;

		init();
	}

	public void init() {

		ObjectInputStream ois;
		try {
			ois = new ObjectInputStream(new FileInputStream(
					Constants.THRESHOLD_CONFIG_DIR + "/"
							+ Constants.THRESHOLD_GROUP_KEY_FILE));

			gk = (GroupKey) ois.readObject();

			ois.close();

			ois = new ObjectInputStream(new FileInputStream(
					Constants.THRESHOLD_CONFIG_DIR + "/"
							+ Constants.THRESHOLD_KEY_SHARE_FILE + serverId));
			keyshare = (KeyShare) ois.readObject();
			ois.close();

			keyshare.postSecretGeneration(gk);
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

	public SigShare decrypt(BigInteger cipher) {
		return keyshare.sign(cipher.toByteArray());
	}



	public boolean verifyDecryptedShare(byte[] data, final SigShare ss) {
		return SigShare.verify(data, ss, gk);
	}

	public boolean verifySignedShare(byte[] data, final SigShare ss) {

		return SigShare.verify(getMessageDigest(data), ss, gk);

	}

	public BigInteger thresholdDecrypt(byte[] data, final SigShare[] sigs) {
		return SigShare.thresholdDecrypt(data, sigs, gk);
	}
	
	

//	public BigInteger thresholdDecrypt(BigInteger cipher,
//			Set<ForwardReadRequestAccept> evidenceSet) {
//		// SigShare[] sigs = new SigShare[evidenceSet.size()];
//
//		SigShare[] shares = new SigShare[gk.getK()];
//		int i = 0;
//		for (ForwardReadRequestAccept frra : evidenceSet) {
//			if (i == gk.getK())
//				break;
//			shares[i++] = frra.getDecryptedShare();
//		}
//
//		return SigShare.thresholdDecrypt(cipher.toByteArray(), shares, gk);
//	}

	public BigInteger thresholdDecrypt(BigInteger cipher, SigShare[] sigs) {

		return SigShare.thresholdDecrypt(cipher.toByteArray(), sigs, gk);
	}
	
	
	public BigInteger thresholdDecrypt(BigInteger b, Set<SigShare> sigShares) {

		// Get t+1 sigshares to sign message
		SigShare[] shares = new SigShare[gk.getK()];
		int i = 0;
		for (SigShare ss : sigShares) {
			if (i == gk.getK())
				break;
			if(ss != null)
				shares[i++] = ss;
		}

		println(shares.length);

		return SigShare.thresholdDecrypt(b.toByteArray(), shares, gk);
	}

	public BigInteger thresholdSign(byte[] b, Set<SigShare> sigShares) {

		// Get t+1 sigshares to sign message
		SigShare[] shares = new SigShare[gk.getK()];
		int i = 0;
		for (SigShare ss : sigShares) {
			if (i == gk.getK())
				break;
			if(ss != null)
				shares[i++] = ss;
		}

		println(shares.length);

		BigInteger digitalSig =  SigShare.thresholdDecrypt(getMessageDigest(b), shares, gk);
		if(!verifySignature(b, digitalSig))
			return null;
		return digitalSig;
	}

	public BigInteger thresholdSign(byte[] b, SigShare[] sigShares) {

		byte[] bi = getMessageDigest(b);
		return SigShare.thresholdDecrypt(bi, sigShares, gk);

	}

	public SigShare sign(Object b) {
		byte[] data = SerializationUtil.serialize(b);

		return keyshare.sign(getMessageDigest(data));

	}

	public SigShare sign(byte[] b) {

		return keyshare.sign(getMessageDigest(b));

	}

	public boolean verifySignature(byte[] data, BigInteger signature) {

		byte[] bi = getMessageDigest(data);

		// /BigInteger recdSign = new
		// BigInteger(ccm.getSerializedMessageSignature());
		BigInteger recdHash = signature.modPow(gk.getExponent(),
				gk.getModulus());

		BigInteger hash = new BigInteger(1, bi);
		println("Verify signature : Data to verify : "
				+ new BigInteger(1, bi).toString());

		println(new BigInteger(bi).equals(recdHash));

		return hash.equals(recdHash);

	}

	private void println(Object equals) {
		// TODO Auto-generated method stub

	}

	public byte[] getMessageDigest(byte[] b) {
		try {
			MessageDigest mDigest = MessageDigest.getInstance("SHA");
			mDigest.update(b);
			return mDigest.digest();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return null;
	}

}
