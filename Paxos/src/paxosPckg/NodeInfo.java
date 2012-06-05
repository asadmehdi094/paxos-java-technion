package paxosPckg;

import java.io.Serializable;

public class NodeInfo implements Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -4294721327944137330L;
	//contains BASIC golbal node info.
	
	public connectionInfo connInfo;
	private boolean _isAcceptor;
	
	public NodeInfo(connectionInfo conn,boolean isAcceptor)
	{
		this.connInfo = conn;
	
		this._isAcceptor = isAcceptor;
		
	}
	
	public boolean isAcceptor()
	{
		return _isAcceptor;
	}
	

}
