package threshsig;

import java.math.BigInteger;

public class PRSGroupKey extends GroupKey {
	
	private BigInteger m;
	
	public PRSGroupKey(int k, int l, int keysize, BigInteger e, BigInteger n, BigInteger m) {
		super(k, l, keysize, e, n);
		this.setM(m);
	}

	public PRSGroupKey(int k, int l, int keysize, BigInteger v, BigInteger e,
			BigInteger n, BigInteger m) {
		super(k, l, keysize, v, e, n);
		this.setM(m);
	}



	public BigInteger getM() {
		return m;
	}



	public void setM(BigInteger m) {
		this.m = m;
	}



	/**
	 * 
	 */
	private static final long serialVersionUID = -8363771441313485772L;

}
