package test;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Random;

import junit.framework.Assert;

import org.junit.BeforeClass;
import org.junit.Test;

import client.ClientStub;
import client.messages.CODEXClientMessage;
import client.messages.CODEXClientMessageType;
import client.messages.ClientReadRequest;

import threshsig.Base64Coder;
import threshsig.GroupKey;
import utils.KeyUtility;
import utils.SerializationUtil;

import main.Constants;
import main.FileOperations;

public class ClientStubTest {
	private static ClientStub cs;
	private static GroupKey gk;

	@BeforeClass
	public static void init() {
		cs = new ClientStub(1, 2, 10000, 0);
		ObjectInputStream ois;

	}

	@Test
	public void testSignature() {
		try {
			// PrivateKey privateKey =
			// KeyUtility.getPrivateKey(Constants.CONFIG_DIR
			// + "/" + Constants.CLIENT_PRIVATE_KEY_FILE + cs.getClientId());

			byte data[] = new byte[128];
			new Random().nextBytes(data);
			// cs.signMessage(data);

			CODEXClientMessage cm = new CODEXClientMessage(
					new Random().nextInt(), 1,
					CODEXClientMessageType.CLIENT_READ_SECRET_REQUEST, data);
			// cs.sendMessage(cm, true);

			// CODEXMessage cm2 = new CODEXMessage(cm.getSerializedMessage());

			PublicKey publicKey = KeyUtility
					.getPublicKey(Constants.CONFIG_DIR + "/"
							+ Constants.CLIENT_PUBLIC_KEY_FILE
							+ cs.getClientId());

			Signature signatureEngine = Signature.getInstance("SHA1withRSA");
			signatureEngine.initVerify(publicKey);
			signatureEngine.update(cm.getSerializedMessage());
			Assert.assertTrue(signatureEngine.verify(cm
					.getSerializedMessageSignature()));

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(final String[] args) {
		if (args.length < 4) {
			System.out.println("Usage : CS l k serverSocketPort clientId");
			return;
		}

		Thread t = new Thread() {
			@Override
			public void run() {
				ClientStub cs = new ClientStub(Integer.parseInt(args[0]),
						Integer.parseInt(args[1]), Integer.parseInt(args[2]),
						Integer.parseInt(args[3]));
				// byte data[] = new byte[128];
				// new Random().nextBytes(data);
				// cs.signMessage(data);

				// BigInteger enc = new BigInteger("blindingfactor".getBytes());
				// //System.out.println(enc);
				// ClientReadRequest crr = new ClientReadRequest(enc, "test");
				//
				// byte data[] = SerializationUtil.serialize(crr);
				//
				// CODEXClientMessage cm = new CODEXClientMessage(
				// new Random().nextInt(), 0,
				// CODEXClientMessageType.CLIENT_READ_SECRET_REQUEST, data);
				//cs.getSecret("test");

				int MAX_TRIES = 5;
				int tries = 0;
				while(!cs.setSecret("test3", "helloworld4") && tries++ < MAX_TRIES);
				
				tries = 0;
				while(cs.getSecret("test3") == null && tries++ < MAX_TRIES);
				//cs.getSecret("test3");
			}
		};

		t.start();
		try {
			t.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
