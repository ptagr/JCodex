package test;

import java.util.Arrays;
import java.util.Random;

import junit.framework.Assert;

import main.CustomBigInteger;

import org.junit.Test;

public class BITest {
	@Test
	public void testNegative(){
		int RUNS = 1000000;
		for(int i=0;i<RUNS;i++){
		byte[] b = new byte[256];
		(new Random()).nextBytes(b);
		b[0] = 0;
		b[1] = 0;
		//b[0] = 1; //negative
		CustomBigInteger bi = new CustomBigInteger(b);
		
		
//		System.out.println(b.length+Arrays.toString(b));
//		System.out.println(bi.toByteArray().length+Arrays.toString(bi.toByteArray()));
//		System.out.println();
		Assert.assertTrue(Arrays.equals(b, bi.toByteArray()));
		}
		
	}
}
