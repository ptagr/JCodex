package proactive.messages;

import java.io.Serializable;
import java.util.HashMap;

import server.SecretShare;

public class DumpStateResponseMessage implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 5711113728007881562L;
	
	private PRSMessage prsMessage;
	
	private String dumpStateFileName;
	
	private HashMap<String, SecretShare> shareDB;

	public DumpStateResponseMessage(PRSMessage prsMessage,
			String dumpStateFileName, HashMap<String, SecretShare> shareDB) {
		super();
		this.prsMessage = prsMessage;
		this.dumpStateFileName = dumpStateFileName;
		this.setShareDB(shareDB);
	}

	public String getDumpStateFileName() {
		return dumpStateFileName;
	}

	public void setDumpStateFileName(String dumpStateFileName) {
		this.dumpStateFileName = dumpStateFileName;
	}

	public PRSMessage getPrsMessage() {
		return prsMessage;
	}

	public void setPrsMessage(PRSMessage prsMessage) {
		this.prsMessage = prsMessage;
	}

	public HashMap<String, SecretShare> getShareDB() {
		return shareDB;
	}

	public void setShareDB(HashMap<String, SecretShare> shareDB) {
		this.shareDB = shareDB;
	}
	
	
}
