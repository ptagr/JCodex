package test;

import proactive.ProactiveRecoveryServer;

public class PRSServerTest {
	public static void main(String[] args) {
		if (args.length < 4) {
			System.out.println("Usage : PST serverSocketPort prsId k l");
			return;
		}
		
		ProactiveRecoveryServer prs = new ProactiveRecoveryServer(Integer.parseInt(args[1]), Integer.parseInt(args[0]),Integer.parseInt(args[2]),Integer.parseInt(args[3]));
		Thread t = new Thread(prs);
		t.start();
		try {
			t.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//prs.sendDumpMessage();
		
	}

}
