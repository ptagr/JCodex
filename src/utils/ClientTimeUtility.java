package utils;

public class ClientTimeUtility {

	public static int timeOut = 10000; // time in mSec
	//public static int clientTimeOut = 4*timeOut; // time in mSec
	public long milliSec;

	public ClientTimeUtility() {
		super();
		this.milliSec = System.currentTimeMillis();
	}

	public ClientTimeUtility(long milliSec) {
		super();
		this.milliSec = milliSec;
	}

	public long delta() {
		return System.currentTimeMillis() - milliSec;
	}

	public boolean timerHasNotExpired() {
		return System.currentTimeMillis() - milliSec < timeOut;
	}
	
	public void reset(){
		this.milliSec = System.currentTimeMillis();
	}
}
