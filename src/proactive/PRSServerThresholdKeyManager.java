package proactive;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.util.Set;

import main.Constants;
import threshsig.GroupKey;
import threshsig.KeyShare;
import threshsig.PRSGroupKey;
import threshsig.Poly;
import threshsig.ThreshUtil;

public class PRSServerThresholdKeyManager {
	private final int serverId;
	private PRSGroupKey prsgk;
	private KeyShare keyshare;

	public PRSGroupKey getPrsgk() {
		return prsgk;
	}

	public int getServerId() {
		return serverId;
	}

	PRSServerThresholdKeyManager(int serverId) {
		this.serverId = serverId;
		ObjectInputStream ois;
		try {
			ois = new ObjectInputStream(new FileInputStream(
					Constants.THRESHOLD_PRS_CONFIG_DIR + serverId+"/"
							+ Constants.THRESHOLD_PRS_GROUP_KEY_FILE));
			prsgk = (PRSGroupKey) ois.readObject();
			ois.close();

			ois = new ObjectInputStream(new FileInputStream(
					Constants.THRESHOLD_PRS_CONFIG_DIR + serverId + "/"
							+ Constants.THRESHOLD_KEY_SHARE_FILE + serverId));
			keyshare = (KeyShare) ois.readObject();
			ois.close();

			keyshare.postSecretGeneration(prsgk);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	public BigInteger[] generateSecrets() {
		BigInteger[] secrets = new BigInteger[prsgk.getL()];
		BigInteger rand;
		int randbits;

		Poly poly = new Poly(ThreshUtil.ZERO, prsgk.getK() - 1, prsgk.getM());
		randbits = prsgk.getN().bitLength() + ThreshUtil.L1
				- prsgk.getM().bitLength();

		// Generates the values f(i) for 1<=i<=l
		// and add some large multiple of m to each value
		for (int i = 0; i < prsgk.getL(); i++) {
			secrets[i] = poly.eval(i + 1);
			rand = (new BigInteger(randbits, ThreshUtil.getRandom()))
					.multiply(prsgk.getM());
			secrets[i] = secrets[i].add(rand);
		}
		
		return secrets;
	}

	public BigInteger updateKeyshare(Set<BigInteger> secrets) {
		BigInteger oldSecret = keyshare.getSecret();
		BigInteger newSecret = new BigInteger(oldSecret.toString());
		for (BigInteger secret : secrets)
			newSecret.add(secret).mod(prsgk.getM());
		BigInteger v = prsgk.getV().modPow(newSecret, prsgk.getN());
		keyshare = new KeyShare(keyshare.getId(), newSecret);
		keyshare.setVerifier(v);
		return v;
	}

	public void updateVerifiers(BigInteger[] verifiers) {
		prsgk.setVi(verifiers);
		GroupKey gk = new GroupKey(prsgk.getK(), prsgk.getL(),
				prsgk.getKeysize(), prsgk.getExponent(), prsgk.getN());
		gk.setV(prsgk.getV());
		gk.setVi(verifiers);
		keyshare.postSecretGeneration(gk);

		// Write the key share, group key, prsgroupkey onto file system
		try {
			
			//Write group key in server config dir
			ObjectOutputStream oos = new ObjectOutputStream(
					new FileOutputStream(Constants.THRESHOLD_CONFIG_DIR + serverId + "/"
							+ Constants.THRESHOLD_GROUP_KEY_FILE));
			oos.writeObject(gk);
			oos.flush();
			oos.close();
			
			
			//Write group key in prs config dir
			 oos = new ObjectOutputStream(
					new FileOutputStream(Constants.THRESHOLD_PRS_CONFIG_DIR + serverId + "/"
							+ Constants.THRESHOLD_GROUP_KEY_FILE));
			oos.writeObject(gk);
			oos.flush();
			oos.close();

			oos = new ObjectOutputStream(new FileOutputStream(
					Constants.THRESHOLD_PRS_CONFIG_DIR+ serverId + "/"
							+ Constants.THRESHOLD_PRS_GROUP_KEY_FILE));
			oos.writeObject(prsgk);
			oos.flush();
			oos.close();

			oos = new ObjectOutputStream(new FileOutputStream(
					Constants.THRESHOLD_PRS_CONFIG_DIR+ serverId + "/"
							+ Constants.THRESHOLD_KEY_SHARE_FILE
							+ this.getServerId()));
			oos.writeObject(keyshare);
			oos.flush();
			oos.close();
			
			oos = new ObjectOutputStream(new FileOutputStream(
					Constants.THRESHOLD_CONFIG_DIR+ serverId  + "/"
							+ Constants.THRESHOLD_KEY_SHARE_FILE
							+ this.getServerId()));
			oos.writeObject(keyshare);
			oos.flush();
			oos.close();
			
		} catch (IOException ie) {
			ie.printStackTrace();
		}
	}

}
