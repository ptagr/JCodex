package test;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.math.BigInteger;
import java.util.HashSet;
import java.util.Set;

import main.Constants;

import server.SecretShare;
import server.Server;
import threshsig.GroupKey;

public class ServerTest {
	public static void main(String args[]) {
		GroupKey gk;
		ObjectInputStream ois;

		try {

			ois = new ObjectInputStream(new FileInputStream(
					Constants.THRESHOLD_CONFIG_DIR + "/"
							+ Constants.THRESHOLD_GROUP_KEY_FILE));
			gk = (GroupKey) ois.readObject();

			ois.close();
			if (args.length < 5) {
				System.out
						.println("Usage : SCM l k serverSocketPort serverId <clientids>");
				return;
			}

			Set<Integer> clientIds = new HashSet<Integer>();
			for (int i = 4; i < args.length; i++) {
				clientIds.add(Integer.parseInt(args[i]));
			}

			Server s = new Server(Integer.parseInt(args[1]),
					Integer.parseInt(args[0]), Integer.parseInt(args[2]),
					Integer.parseInt(args[3]), clientIds);
			
			
			String secret = "helloworld";

			byte[] data2 = secret.getBytes();

			BigInteger b = new BigInteger(1, data2);
			BigInteger cipher = b.modPow(gk.getExponent(), gk.getModulus());

			System.out.println("Stored secret :" + secret.toString());
//			BigInteger cipher = secret
//					.modPow(gk.getExponent(), gk.getModulus());
//			s.addSecret("test", new SecretShare(BigInteger.ONE, cipher));
			
			s.addSecret("test", new SecretShare(BigInteger.ONE, cipher));

			// for (int i = 0; i < Integer.parseInt(args[0]); i++) {
			new Thread(s).start();
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

		// }

	}
}
