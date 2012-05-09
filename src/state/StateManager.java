package state;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.HashMap;

import main.Constants;
import server.SecretShare;

public class StateManager {
	// The db to store the shares -> in memory as of now
	private final HashMap<String, SecretShare> shareDB;
	private final int serverId;

	public StateManager(int serverId) {
		this.shareDB = new HashMap<String, SecretShare>();
		this.serverId = serverId;
	}

	public StateManager(int serverId, HashMap<String, SecretShare> state) {
		this.shareDB = state;
		this.serverId = serverId;
	}

	public String dumpServerState() {

		String fname = Constants.STATE_DIR + "/" + Constants.STATE_DUMP_FILE
				+ this.getServerId();
		println("Dumping state to file " + fname);

		try {
			ObjectOutputStream oos = new ObjectOutputStream(
					new FileOutputStream(fname));
			oos.writeObject(shareDB);
			oos.flush();
			oos.close();

			// BufferedWriter out = new BufferedWriter(
			// new FileWriter(fname));
			// out.write(shareDB.size());
			// out.newLine();
			// for(String str : shareDB.keySet()){
			// out.write(str);
			//
			// SecretShare ss = shareDB.get(str);
			// out.write(ss.getTimestamp().intValue());
			// out.newLine();
			// out.write(ss.getSecret().toString());
			// out.newLine();
			//
			// }
			//
			// out.flush();
			// out.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// FileOperations.writeToFile(fname,
		// SerializationUtil.serialize(shareDB),
		// false);
		return fname;
	}

	private void println(String string) {
		System.out.println("State Manager " + this.getServerId() + " : "
				+ string);

	}

	public int getServerId() {
		return serverId;
	}

	public SecretShare getSecretShare(String dataId) {
		return this.shareDB.get(dataId);
	}

	public SecretShare update(String dataId, SecretShare ss) {
		return this.shareDB.put(dataId, ss);
	}

	public void printState() {
		for (String s : shareDB.keySet()) {
			println(s + " : " + shareDB.get(s));
		}
	}

	public Integer getTimestampFromDB(String dataId) {
		SecretShare ss = shareDB.get(dataId);
		if (ss == null) {
			return 1;
		} else
			return ss.getTimestamp();
	}

	public HashMap<String, SecretShare> getShareDB() {
		return shareDB;
	}

}
