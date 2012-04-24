package utils;

public class TimeUtility {

	public int timeOut = 5000; // time in mSec
	//public static int clientTimeOut = 4*timeOut; // time in mSec
	public long milliSec;

	public TimeUtility() {
		super();
		this.milliSec = System.currentTimeMillis();
	}

	public TimeUtility(long milliSec) {
		super();
		this.milliSec = milliSec;
	}

	public long delta(long time) {
		return System.currentTimeMillis() - milliSec;
	}

	public boolean timerHasNotExpired() {
		return System.currentTimeMillis() - milliSec < timeOut;
	}
	
	public void reset(){
		this.milliSec = System.currentTimeMillis();
	}
}
