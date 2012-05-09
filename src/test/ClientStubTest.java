package test;

import java.security.PublicKey;
import java.security.Signature;
import java.util.Random;

import junit.framework.Assert;
import main.Constants;

import org.junit.BeforeClass;
import org.junit.Test;

import utils.KeyUtility;
import client.ClientStub;
import client.messages.CODEXClientMessage;
import client.messages.CODEXClientMessageType;

public class ClientStubTest {
	private static ClientStub cs;

	@BeforeClass
	public static void init() {
		cs = new ClientStub(1, 2, 10000, 0);

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
					CODEXClientMessageType.CLIENT_READ_REQUEST, data);
			// cs.sendMessage(cm, true);

			// CODEXMessage cm2 = new CODEXMessage(cm.getSerializedMessage());

			PublicKey publicKey = KeyUtility
					.getPublicKey(Constants.CONFIG_DIR + "0/"
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
				// CODEXClientMessageType.CLIENT_READ_REQUEST, data);
				//cs.getSecret("test");

				int MAX_TRIES = 5;
				int tries = 0;
				
				//cs.getSecret("test");
				cs.setSecret("test", "helloworld1");
				cs.setSecret("test1", "helloworld2");
//				cs.setSecret("test2", "helloworld3");
//				cs.setSecret("test3", "helloworld4");
//				cs.setSecret("test4", "helloworld5");
//				cs.setSecret("test5", "helloworld6");
//
//				cs.setSecret("test6", "helloworld7");
//				cs.setSecret("test7", "helloworld8");
//				cs.setSecret("test8", "helloworld9");
//				cs.getSecret("test5");
				
				//cs.getTimeStamp("test");
				//while(!cs.setSecret("test3", "helloworld4") && tries++ < MAX_TRIES);
				
				tries = 0;
				//while(cs.getSecret("test3") == null && tries++ < MAX_TRIES);
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
