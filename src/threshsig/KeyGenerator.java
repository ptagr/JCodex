package threshsig;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashSet;
import java.util.Set;

import main.Constants;

import org.junit.Test;

public class KeyGenerator {
	// private static int keysize = 1024;
	//
	// private static int k = 2;
	//
	// private static int l = 4;

	private static Dealer d;

	public static void main(String[] args) throws FileNotFoundException,
			IOException {
		testInitDealer(1024, 5, 2);

		Set<Integer> serverIds = new HashSet<Integer>();
		for (int i = 0; i < 5; i++)
			serverIds.add(i);
		
		Set<Integer> clientIds = new HashSet<Integer>();
		for (int i = 0; i < 2; i++)
			clientIds.add(i);
		for(Integer c : clientIds){
			ObjectOutputStream oos = new ObjectOutputStream(
					new FileOutputStream(Constants.CLIENT_CONFIG_DIR +c+ "/"
							+ Constants.THRESHOLD_GROUP_KEY_FILE));
			oos.writeObject(d.getGroupKey());
			oos.flush();
			oos.close();
		}
		
		
		for (Integer s : serverIds) {
			ObjectOutputStream oos = new ObjectOutputStream(
					new FileOutputStream(Constants.THRESHOLD_CONFIG_DIR +s+ "/"
							+ Constants.THRESHOLD_GROUP_KEY_FILE));
			oos.writeObject(d.getGroupKey());
			oos.flush();
			oos.close();

			oos = new ObjectOutputStream(new FileOutputStream(
					Constants.THRESHOLD_PRS_CONFIG_DIR + s + "/"
							+ Constants.THRESHOLD_PRS_GROUP_KEY_FILE));
			oos.writeObject(d.getPRSGroupKey());
			oos.flush();
			oos.close();

			KeyShare[] keys = d.getShares();

			//for (int i = 0; i < keys.length; i++) {
				oos = new ObjectOutputStream(new FileOutputStream(
						Constants.THRESHOLD_CONFIG_DIR + s+"/"
								+ Constants.THRESHOLD_KEY_SHARE_FILE + s));
				oos.writeObject(keys[s]);
				oos.flush();
				oos.close();

				oos = new ObjectOutputStream(new FileOutputStream(
						Constants.THRESHOLD_PRS_CONFIG_DIR + s+ "/"
								+ Constants.THRESHOLD_KEY_SHARE_FILE + s));
				oos.writeObject(keys[s]);
				oos.flush();
				oos.close();
			//}
		}
	}

	// @Test
	public static final void testInitDealer(int keysize, int l, int k) {
		// Initialize a dealer with a keysize
		d = new Dealer(keysize);

		final long start = System.currentTimeMillis();
		long elapsed;
		// Generate a set of key shares
		d.generateKeys(k, l);

		elapsed = System.currentTimeMillis() - start;
		System.out.println("\tKey Gen total (ms): " + elapsed);

		// This is the group key common to all shares, which
		// is not assumed to be trusted. Treat like a Public Key
		// gk = d.getGroupKey();

		// The Dealer has the shares and is assumed trusted
		// This should be destroyed, unless you want to reuse the
		// Special Primes of the group key to generate a new set of
		// shares
		// keys = d.getShares();
	}

	// @Test
	// public void testSerialization() {
	// try {
	//
	// // Make everythg null
	// d = null;
	// keys = null;
	// gk = null;
	//
	// ObjectInputStream ois = new ObjectInputStream(new FileInputStream(
	// "config/groupkey"));
	// gk = (GroupKey) ois.readObject();
	//
	// ois.close();
	//
	// keys = new KeyShare[gk.getL()];
	// for (int i = 0; i < keys.length; i++) {
	// ois = new ObjectInputStream(new FileInputStream(
	// "config/keyshare" + i));
	// keys[i] = (KeyShare) ois.readObject();
	// ois.close();
	// }
	//
	// for (int i = 0; i < keys.length; i++) {
	// keys[i].postSecretGeneration(gk);
	// }
	//
	// } catch (Exception e) {
	// e.printStackTrace();
	// }
	// }

}
