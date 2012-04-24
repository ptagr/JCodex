package threshsig;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class CODEXKeyConfig {
	public static void main(String[] args) {
		final int keysize = 1024;

		final int k = 2;

		final int l = 4;

		Dealer d = new Dealer(1024);

		d.generateKeys(k, l);

		try {
			
			BufferedWriter out = new BufferedWriter(new FileWriter("keys.config"));
			//FileOutputStream fout = new FileOutputStream("");
			
			//First line is no of servers
			out.write(l+","+k+","+keysize);
			out.newLine();
			
			
			//ObjectOutputStream oos = new ObjectOutputStream(fout);
			System.out.println("serializing the group key");
			String temp = toString(d.getGroupKey());
			out.write(temp);
			out.newLine();
			
			//oos.writeObject(d.getGroupKey());

			for (KeyShare ks : d.getShares()) {
				temp = toString(ks);
				out.write(temp);
				out.newLine();
			}
			
			out.flush();
			out.close();
			
			
			BufferedReader br = new BufferedReader(new FileReader("keys.config"));
			String temp2 = br.readLine();
			for(String t : temp2.split(",")){
				System.out.println(t);
			}
			
			temp2 = br.readLine();
			GroupKey gk = (GroupKey) fromString(temp2);
			System.out.println(gk.getK());
			System.out.println(gk.getL());
			
			KeyShare[] shares = new KeyShare[gk.getL()];
			for(int i = 0; i< gk.getL(); i++){
				shares[i] = (KeyShare) fromString(br.readLine());
				shares[i].postSecretGeneration(gk);
			}
			
			
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/** Read the object from Base64 string. */
	public static Object fromString(String s) throws IOException,
			ClassNotFoundException {
		byte[] data = Base64Coder.decode(s);
		ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(
				data));
		Object o = ois.readObject();
		ois.close();
		return o;
	}

	/** Write the object to a Base64 string. */
	public static String toString(Serializable o) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(baos);
		oos.writeObject(o);
		oos.close();
		return new String(Base64Coder.encode(baos.toByteArray()));
	}

}
