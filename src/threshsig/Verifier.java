package threshsig;

import java.io.Serializable;
import java.math.BigInteger;

public class Verifier implements Serializable{
  /**
	 * 
	 */
	private static final long serialVersionUID = 4425676036780693197L;

private BigInteger z;

  private BigInteger c;



  public Verifier(final BigInteger z, final BigInteger c) {
    this.z = z;
    this.c = c;
//    this.shareVerifier = shareVerifier;
//    this.groupVerifier = groupVerifier;
  }

  public BigInteger getZ() {
    return this.z;
  }

//  public BigInteger getShareVerifier() {
//    return this.shareVerifier;
//  }
//
//  public BigInteger getGroupVerifier() {
//    return this.groupVerifier;
//  }

  public BigInteger getC() {
    return this.c;
  }
}
