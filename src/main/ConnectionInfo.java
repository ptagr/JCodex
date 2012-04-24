package main;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class ConnectionInfo {
	private InetAddress inetAddress;
	private int port;

	public ConnectionInfo(InetAddress inetAddress, int port) {
		super();

		this.inetAddress = inetAddress;
		this.port = port;
	}

	public ConnectionInfo(String inetAddress, int port) {
		super();
		try {
			this.inetAddress = InetAddress.getByName(inetAddress);
			this.port = port;
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public InetAddress getInetAddress() {
		return inetAddress;
	}

	public void setInetAddress(InetAddress inetAddress) {
		this.inetAddress = inetAddress;
	}

	public void setInetAddress(String inetAddress) {
		try {
			this.inetAddress = InetAddress.getByName(inetAddress);
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
