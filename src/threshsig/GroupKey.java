package threshsig;

import java.io.Serializable;
import java.math.BigInteger;

/**
 * A GroupKey with information common to all the keyshares. Generated by the
 * Dealer class
 * 
 * Reference: "Practical Threshold Signatures",<br>
 * Victor Shoup (sho@zurich.ibm.com), IBM Research Paper RZ3121, 4/30/99<BR>
 * 
 * @author Steve Weis <sweis@mit.edu>
 */

// TODO: Investigate the security of reusing the key parameters (q,p) with a
// new Poly, since they are computationally expensive
public class GroupKey implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 8238129371761545594L;

	/** (k,l) Parameters. k out of l shares needed for a signature */
	private Integer k, l;

	/** The exponent of the groupKeyPair */
	private BigInteger e;

	/** The RSA modulus of the groupKeyPair */
	private BigInteger n;
	
	private int keysize;

	private BigInteger v;

	private BigInteger[] vi;

	public GroupKey(final int k, final int l, final int keysize,
			final BigInteger v, final BigInteger e, final BigInteger n) {
		this.k = k;
		this.l = l;
		this.v = v;
		this.e = e;
		this.n = n;
		this.setKeysize(keysize);
	}

	public GroupKey(final int k, final int l, final int keysize,
			final BigInteger e, final BigInteger n) {
		this.k = k;
		this.l = l;
		this.e = e;
		this.n = n;
		this.setKeysize(keysize);
	}

	/**
	 * Returns the minimum threshold size
	 * 
	 * @return The minimum threshold size for this group 'k'.
	 */
	public int getK() {
		return this.k;
	}

	/**
	 * Returns the group size
	 * 
	 * @return The size of this key's associated group.
	 */
	public int getL() {
		return this.l;
	}

	/**
	 * Returns the group key modulus
	 * 
	 * @return This group's modulus.
	 */
	public BigInteger getModulus() {
		return this.n;
	}

	/**
	 * Returns the group key exponent
	 * 
	 * @return This group's exponent
	 */
	public BigInteger getExponent() {
		return this.e;
	}

	public BigInteger getV() {
		return v;
	}

	public void setV(BigInteger v) {
		this.v = v;
	}

	public BigInteger[] getVi() {
		return vi;
	}

	public void setVi(BigInteger[] vi) {
		this.vi = vi;
	}

	public BigInteger getE() {
		return e;
	}

	public void setE(BigInteger e) {
		this.e = e;
	}

	public BigInteger getN() {
		return n;
	}

	public void setN(BigInteger n) {
		this.n = n;
	}

	public void setK(int k) {
		this.k = k;
	}

	public void setL(int l) {
		this.l = l;
	}

	public int getKeysize() {
		return keysize;
	}

	public void setKeysize(int keysize) {
		this.keysize = keysize;
	}
	
	
	
}
