package threshsig.testing;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import threshsig.CODEXKeyConfig;
import threshsig.Dealer;
import threshsig.GroupKey;
import threshsig.KeyShare;
import threshsig.SigShare;

public class ThreshTest {
	private static int keysize = 1024;

	private static int k = 2;

	private static int l = 4;

	private static Dealer d;

	private static GroupKey gk;

	private static KeyShare[] keys;

	private static final byte[] data = new byte[1024];

	private static byte[] b;

	private static final SigShare[] sigs = new SigShare[k];

	private static BigInteger privateKey;

	@Before
	public final void setUp() throws NoSuchAlgorithmException {
		(new Random()).nextBytes(data);
		final MessageDigest md = MessageDigest.getInstance("SHA-1");
		b = md.digest(data);
		// testInitDealer();
		testSerialization();
	}

	// @Test
	public final void testInitDealer() {
		// Initialize a dealer with a keysize
		d = new Dealer(keysize);

		final long start = System.currentTimeMillis();
		long elapsed;
		// Generate a set of key shares
		privateKey = d.generateKeys(k, l);

		elapsed = System.currentTimeMillis() - start;
		System.out.println("\tKey Gen total (ms): " + elapsed);

		// This is the group key common to all shares, which
		// is not assumed to be trusted. Treat like a Public Key
		gk = d.getGroupKey();

		// The Dealer has the shares and is assumed trusted
		// This should be destroyed, unless you want to reuse the
		// Special Primes of the group key to generate a new set of
		// shares
		keys = d.getShares();
	}

	@Test
	public void testVerifySignatures() throws NoSuchAlgorithmException {
		System.out.println("Attempting to verify a valid set of signatures...");
		// Pick a set of shares to attempt to verify
		// These are the indices of the shares
		final int[] S = { 3, 5, 1, 2, 10, 7 };

		for (int i = 0; i < S.length; i++)
			sigs[i] = keys[S[i]].sign(b);

		BigInteger combinedSignature = SigShare.verify(b, sigs, gk);

		Assert.assertNotNull(combinedSignature);

		BigInteger data = combinedSignature.modPow(gk.getExponent(),
				gk.getModulus());

		System.out.println("Private key d bit length : "
				+ privateKey.bitLength());

		BigInteger origSign = new BigInteger(b).modPow(privateKey,
				gk.getModulus());

		System.out.println("Orig signature bit length : "
				+ origSign.bitLength());

		BigInteger hash = origSign.modPow(gk.getExponent(), gk.getModulus());

		System.out.println("Orig signature hash bit length : "
				+ hash.bitLength());

		// Assert.assertTrue(combinedSignature.equals(originalSignature));
		System.out.println("Combined signature bit length : "
				+ data.bitLength());
		System.out.println("MOdulus bit length : "
				+ gk.getModulus().bitLength());
		System.out
				.println("Hash bit length : " + new BigInteger(b).bitLength());
		System.out.println("Hash bit length in byte array: " + b.length);
		System.out.println("data : " + data + "\nb : " + new BigInteger(b));
		Assert.assertTrue(data.equals(new BigInteger(b)));

	}

	@Test
	public void testDecryption() throws NoSuchAlgorithmException {
		int RUNS = 1;
		for (int i = 0; i < RUNS; i++) {
			String secret = "helloworld";

			// byte[] data2 = new byte[100];
			// (new Random()).nextBytes(data2);
			byte[] data2 = secret.getBytes();
			// data2[0] = 0;
			// Encrypt data
			BigInteger b = new BigInteger(1, data2);
			BigInteger cipher = b.modPow(gk.getExponent(), gk.getModulus());

			// BigInteger origData = cipher.modPow(privateKey, gk.getModulus());
			//
			// System.out.println("cipher bytelength : "+cipher.bitLength()/8);
			// System.out.println("b 		 : " + b);
			// System.out.println("OrigData : " + origData);
			// Assert.assertTrue(origData.equals(b));

			// BigInteger c = new BigInteger(1, cip)
			final int[] S = { 1, 3 };
			for (int j = 0; j < k; j++) {
				sigs[j] = keys[S[j]].sign(cipher.toByteArray());
				System.out.println(sigs[j]);
			}

			System.out.println("Data to decrypt : " + cipher);

			// BigInteger combinedSignature =
			// SigShare.verify(cipher.toByteArray(), sigs, gk);
			BigInteger dcryptedData = SigShare.thresholdDecrypt(
					cipher.toByteArray(), sigs, gk);
			if (dcryptedData == null)
				System.out.println("Sig Failed to verify correctly");
			else {
				System.out.println("DD : " + dcryptedData);
				// BigInteger sigHash =
				// combinedSignature.modPow(gk.getExponent(),
				// gk.getModulus());

				System.out.println("Orig message in BigInteger format : " + b);
				System.out.println("Decr message in BigInteger format : "
						+ dcryptedData);

				System.out.println(new String(dcryptedData.toByteArray()));

				// Assert.assertTrue(combinedSignature.equals(originalSignature));
				Assert.assertTrue(b.equals(dcryptedData));

				// Cannot compare byte arrays directly, returns false
				// System.out.println(Arrays.equals(b,
				// combinedSignature.toByteArray()));

			}
		}
	}

	@Test
	public void testVerifySignaturesAgain() {
		System.out.println("Attempting to verify a different set of shares...");

		// Create k sigs to verify using different keys
		// Set<Integer> set = new HashSet<Integer>();
		//
		// Random r = new Random();
		// // int i = 0;
		// while (set.size() < k) {
		// set.add(r.nextInt(k));
		// }
		BigInteger x = new BigInteger(
				"7361885727555794414947809545226100608227088470829435065167413689255607191512620907960678085152959074406122417530756446705084986556933047231680676160967081536646970426511487233932393591421110923147427188915412430511175244962486289840599619029421017291267965306328545798503613019719805087266648522901644981543413097258796689984334948519349650765659914052910885279236948404121599631761684779449660852051292752978471248160838936498996144906418946639889795203048902529600112430613227514453373321382712107633494998671817432382554960814011710215538198878019006830068105472539729830036868732528469727598884811090915683583204");

		// Integer[] T = set.toArray(new Integer[] {});
		Integer[] T = { 1, 3 };
		for (int i = 0; i < k; i++)
			sigs[i] = keys[T[i]].sign(b);

		BigInteger combinedSignature = SigShare.thresholdDecrypt(b, sigs, gk);
		// BigInteger combinedSignature = SigShare.verify(b, sigs, gk);

		Assert.assertNotNull(combinedSignature);

		BigInteger data = combinedSignature.modPow(gk.getExponent(),
				gk.getModulus());
		System.out.println("data : " + data + "\nb : " + new BigInteger(1, b));
		// Assert.assertTrue(combinedSignature.equals(originalSignature));
		Assert.assertTrue(data.equals(new BigInteger(1, b)));

	}

	@Test
	public void testVerifyBadSignature() {
		b = "corrupt data".getBytes();
		sigs[3] = keys[3].sign(b);
		Assert.assertNull(SigShare.verify(b, sigs, gk));
	}

	@Test
	public void testPerformance() throws NoSuchAlgorithmException {

		final int RUNS = 1;
		// final int[] S = { 1, 3 };
		// final int[] S = { 3, 5, 1, 2, 9, 7 };

		Set<Integer> set = new HashSet<Integer>();

		Random r = new Random();
		// int i = 0;
		while (set.size() < k) {
			set.add(r.nextInt(k));
		}

		Integer[] S = set.toArray(new Integer[] {});

		long start = System.currentTimeMillis(), elapsed;
		// for (int i = 0; i < RUNS; i++)
		// sigs[i % k] = keys[S[i % k]].sign(b);
		// elapsed = System.currentTimeMillis() - start;
		// System.out.println("Signing total (" + RUNS + " sigs) (ms): " +
		// elapsed
		// + " Average: " + (float) (elapsed / RUNS));

		start = System.currentTimeMillis();
		int i = 0;
		for (i = 0; i < RUNS; i++) {
			(new Random()).nextBytes(data);
			final MessageDigest md = MessageDigest.getInstance("SHA-1");
			b = md.digest(data);
			// b = new byte[128];
			// (new Random()).nextBytes(b);
			BigInteger origHash = new BigInteger(1, b);

			for (int j = 0; j < k; j++) {
				sigs[j] = keys[S[j]].sign(b);
				System.out.println(sigs[j]);
			}

			BigInteger combinedSignature = SigShare.verify(b, sigs, gk);
			if (combinedSignature == null)
				System.out.println("Sig Failed to verify correctly");
			else {

				BigInteger sigHash = combinedSignature.modPow(gk.getExponent(),
						gk.getModulus());

				System.out.println("Orig Hash in BigInteger format : "
						+ origHash);
				System.out.println("Sig  Hash in BigInteger format : "
						+ sigHash);

				// Assert.assertTrue(combinedSignature.equals(originalSignature));
				Assert.assertTrue(sigHash.equals(origHash));

				// Cannot compare byte arrays directly, returns false
				// System.out.println(Arrays.equals(b,
				// combinedSignature.toByteArray()));

			}
		}

		System.out.println("No of successful runs : " + i);
		elapsed = System.currentTimeMillis() - start;
		System.out.println("Verification total (" + RUNS + " sigs) (ms): "
				+ elapsed + " Average: " + (float) (elapsed / RUNS));
	}

	@Test
	public void testSerialization() {
		try {

			// ObjectOutputStream oos = new ObjectOutputStream(
			// new FileOutputStream("config/groupkey"));
			// oos.writeObject(d.getGroupKey());
			// oos.flush();
			// oos.close();
			//
			// for (int i = 0; i < keys.length; i++) {
			// oos = new ObjectOutputStream(new FileOutputStream(
			// "config/keyshare" + i));
			// oos.writeObject(keys[i]);
			// oos.flush();
			// oos.close();
			// }

			// Make everythg null
			d = null;
			keys = null;
			gk = null;

			ObjectInputStream ois = new ObjectInputStream(new FileInputStream(
					"config/groupkey"));
			gk = (GroupKey) ois.readObject();

			ois.close();

			keys = new KeyShare[gk.getL()];
			for (int i = 0; i < keys.length; i++) {
				ois = new ObjectInputStream(new FileInputStream(
						"config/keyshare" + i));
				keys[i] = (KeyShare) ois.readObject();
				ois.close();
			}

			for (int i = 0; i < keys.length; i++) {
				keys[i].postSecretGeneration(gk);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
