package main;

import java.io.Serializable;
import java.math.BigInteger;

public class CustomBigInteger extends BigInteger implements Serializable{

	private int precedingZeroesCount = 0;
	private boolean isNegative = false; 
	public CustomBigInteger(byte[] val) {
		super(1, val);
		if(val.length > 0)
			isNegative = (val[0] < 0);
		for(int i=0;i<val.length&&val[i]==0;i++,precedingZeroesCount++);
		precedingZeroesCount--;
	}
	
	
	
	@Override
	public byte[] toByteArray() {
		byte[] result = super.toByteArray();
		int temp = precedingZeroesCount;
		
		if(isNegative){
			byte[] newResult = new byte[result.length-1+temp];
			
			for(int j=0; j<newResult.length; j++){
				newResult[j] = result[j+1];
			}
			return newResult;
		}else if(temp > 0){
			if(result[0] != 0)
				temp++;
			byte[] newResult = new byte[result.length+temp];
			int i =0;
			for(i=0; i<temp; i++){
				newResult[i]=0;
			}
			for(int j=0; j<result.length; j++){
				newResult[j+i] = result[j];
			}
			return newResult;
		}
		return result;
			
	};

	/**
	 * 
	 */
	private static final long serialVersionUID = 4697712728914815794L;

}

