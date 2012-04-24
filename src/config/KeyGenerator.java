package config;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.HashSet;
import java.util.Set;

import main.Constants;
import main.FileOperations;
import threshsig.Base64Coder;

public class KeyGenerator {
	public static void main(String[] args) {
		// Usage : keygen keysize serverNum clientNum
		if (args.length < 3) {
			System.out.println("Usage : keysize serverNum clientNum");
			System.exit(0);
		}
		int keySize = Integer.parseInt(args[0]);
		int serverNum = Integer.parseInt(args[1]);
		int clientNum = Integer.parseInt(args[2]);

		Set<Integer> serverIds = new HashSet<Integer>();
		for (int i = 0; i < serverNum; i++)
			serverIds.add(i);

		Set<Integer> clientIds = new HashSet<Integer>();
		for (int i = 0; i < clientNum; i++)
			clientIds.add(i);

		generateKeyPairs(keySize, clientIds, serverIds);

	}

	private static void generateKeyPairs(int keySize, Set<Integer> clientIds,
			Set<Integer> serverIds) {

		// Generate the client key-pairs
		for (Integer clientId : clientIds) {

			try {
				KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
				keyGen.initialize(keySize, new SecureRandom());
				KeyPair kp = keyGen.generateKeyPair();

				// Write private key to config file
				PrivateKey privateKey = kp.getPrivate();
				
				PKCS8EncodedKeySpec pkcs8EncodedKeySpec = new PKCS8EncodedKeySpec(
						privateKey.getEncoded());
//				String privateKeyStr = new String(Base64Coder.encode(privateKey
//						.getEncoded()));
				FileOperations.writeToFile(Constants.CONFIG_DIR + "/"
						+ Constants.CLIENT_PRIVATE_KEY_FILE + clientId,
						pkcs8EncodedKeySpec.getEncoded(), false);

				// Write public key to config file
				PublicKey publicKey = kp.getPublic();
				X509EncodedKeySpec x509EncodedKeySpec = new X509EncodedKeySpec(
						publicKey.getEncoded());
//				String publicKeyStr = new String(Base64Coder.encode(publicKey
//						.getEncoded()));
				FileOperations.writeToFile(Constants.CONFIG_DIR + "/"
						+ Constants.CLIENT_PUBLIC_KEY_FILE + clientId,
						x509EncodedKeySpec.getEncoded(), false);

			} catch (NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

		// Generate the server key-pairs
		for (Integer serverId : serverIds) {

			try {
				KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
				keyGen.initialize(keySize, new SecureRandom());
				KeyPair kp = keyGen.generateKeyPair();

				// Write private key to config file
				PrivateKey privateKey = kp.getPrivate();
				PKCS8EncodedKeySpec pkcs8EncodedKeySpec = new PKCS8EncodedKeySpec(
						privateKey.getEncoded());
//				String privateKeyStr = new String(Base64Coder.encode(privateKey
//						.getEncoded()));
				FileOperations.writeToFile(Constants.CONFIG_DIR + "/"
						+ Constants.SERVER_PRIVATE_KEY_FILE + serverId,
						pkcs8EncodedKeySpec.getEncoded(), false);

				// Write public key to config file
				PublicKey publicKey = kp.getPublic();
				X509EncodedKeySpec x509EncodedKeySpec = new X509EncodedKeySpec(
						publicKey.getEncoded());
//				String publicKeyStr = new String(Base64Coder.encode(publicKey
//						.getEncoded()));
				FileOperations.writeToFile(Constants.CONFIG_DIR + "/"
						+ Constants.SERVER_PUBLIC_KEY_FILE + serverId,
						x509EncodedKeySpec.getEncoded(), false);

			} catch (NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

	}
}
