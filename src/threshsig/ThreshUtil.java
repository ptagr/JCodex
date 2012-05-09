package threshsig;

import java.math.BigInteger;
import java.security.SecureRandom;

public class ThreshUtil {
  // Constants and variables
  // ............................................................................
  public final static BigInteger ZERO = BigInteger.ZERO;

  public final static BigInteger ONE = BigInteger.ONE;

  public final static BigInteger TWO = BigInteger.valueOf(2L);

  public final static BigInteger FOUR = BigInteger.valueOf(4L);

  /** Fermat prime F4. */
  public final static BigInteger F4 = BigInteger.valueOf(0x10001L);

  /** An arbitrary security parameter for generating secret shares */
  public final static int L1 = 128;

  private static final SecureRandom random = new SecureRandom();

  public static SecureRandom getRandom() {
    return random;
  }
}
