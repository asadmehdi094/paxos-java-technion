package paxosPckg;

public class acceptReq extends Msg{
	/**
	 * 
	 */
	private static final long serialVersionUID = -2119299273366858668L;
	public  String value;
	public  ballot blt;
	public  int lapIndex;
	public  int machineIndex;
	public  int uniqIndex;
	
	public acceptReq(String val ,ballot tblt,int lapIdx,int mIndex,int uIndex)
	{
		value=val;
		blt = tblt;
		lapIndex = lapIdx;
		machineIndex = mIndex;
		uniqIndex = uIndex;
		
	}

}
