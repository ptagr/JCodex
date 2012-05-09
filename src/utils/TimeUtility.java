package utils;

public class TimeUtility {

	public static int timeOut = 10000; // time in mSec
	//public static int clientTimeOut = 4*timeOut; // time in mSec
	public int localtimeOut;
	public long milliSec;

	public TimeUtility() {
		super();
		this.milliSec = System.currentTimeMillis();
		this.localtimeOut = timeOut;
	}
	
	public TimeUtility(int localtimeOut) {
		super();
		this.milliSec = System.currentTimeMillis();
		this.localtimeOut = localtimeOut;
	}

	public TimeUtility(long milliSec) {
		super();
		this.milliSec = milliSec;
	}

	public long delta() {
		return System.currentTimeMillis() - milliSec;
	}

	public boolean timerHasNotExpired() {
		return System.currentTimeMillis() - milliSec < localtimeOut;
	}
	
	public void reset(){
		this.milliSec = System.currentTimeMillis();
	}
}
