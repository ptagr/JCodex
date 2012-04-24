package utils;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import main.FileOperations;

public class KeyUtility { 
	
	public static PrivateKey getPrivateKey(String file) {
		return getPrivateKey(file, "RSA");
	}
	
	public static PrivateKey getPrivateKey(String file, String algorithm) {

		try {
			byte[] encodedPrivateKey = FileOperations.readBytesFromFile(file);
			PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(
					encodedPrivateKey);
			KeyFactory keyFact = KeyFactory.getInstance(algorithm);
			return keyFact.generatePrivate(privateKeySpec);
		} catch (InvalidKeySpecException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	public static PublicKey getPublicKey(String file) {
		return getPublicKey(file, "RSA");
	}
	
	public static PublicKey getPublicKey(String file, String algorithm) {

		try {
			byte[] encodedPublicKey = FileOperations.readBytesFromFile(file);
			X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(
					encodedPublicKey);
			KeyFactory keyFact = KeyFactory.getInstance(algorithm);
			return keyFact.generatePublic(publicKeySpec);
		} catch (InvalidKeySpecException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
}
