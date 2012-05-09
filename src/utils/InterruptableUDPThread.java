package utils;

import java.net.DatagramSocket;

public class InterruptableUDPThread extends Thread{

	   private final DatagramSocket socket;

	   public InterruptableUDPThread(Runnable r, DatagramSocket socket){
		  super(r);
	      this.socket = socket;
	   }
	   
	   @Override
	   public void interrupt(){
	     super.interrupt();  
	     this.socket.close();
	   }
	}