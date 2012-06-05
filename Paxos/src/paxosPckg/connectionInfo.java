package paxosPckg;

import java.io.Serializable;

public class connectionInfo implements Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1522985759911886179L;
	private int port;
	private String ipAdress;
	
	public connectionInfo(String INip ,int INport)
	{
		port = INport;
		ipAdress = INip;
	}
	
	public int getPort()
	{
		return port;
	}
	
	public void setPort(int INport)
	{
		port=INport;
	}
	
	public String getIP()
	{
		return ipAdress;
	}
	
	public void setIP(String INip)
	{
		ipAdress = INip;
	}

}
