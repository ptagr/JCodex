//package threshsig;
//
//import java.math.BigInteger;
//import java.security.MessageDigest;
//
//public class ThresholdSig {
//	public static void generateSignature(final byte[] data,
//			final SigShare[] sigs, final int k, final int l,
//			final BigInteger n, final BigInteger e) {
//
//		// The signature shares in sigs are all valid
//		// We need to combine the shares to get a single signature
//
//		// Sanity Check - make sure there are at least k unique sigs out of l
//		// possible
//
//		final boolean[] haveSig = new boolean[l];
//		for (int i = 0; i < k; i++) {
//			// debug("Checking sig " + sigs[i].getId());
//			if (sigs[i] == null)
//				throw new ThresholdSigException("Null signature");
//			if (haveSig[sigs[i].getId() - 1])
//				throw new ThresholdSigException("Duplicate signature: "
//						+ sigs[i].getId());
//			haveSig[sigs[i].getId() - 1] = true;
//		}
//		try {
//			final MessageDigest md = MessageDigest.getInstance("SHA");
//
//			BigInteger x = new BigInteger(md.digest(data));
//			
//			for (int i = 0; i < k; i++) {
//				BigInteger xijsquare = x.modPow(exponent, m)
//			}
//			
//			
//		} catch (final java.security.NoSuchAlgorithmException ex) {
//			SigShare.debug("Provider could not locate SHA message digest .");
//			ex.printStackTrace();
//		}
//
//	}
//
//}
