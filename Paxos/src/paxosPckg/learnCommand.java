package paxosPckg;

public class learnCommand extends Msg{
	/**
	 * 
	 */
	private static final long serialVersionUID = 664835588333378420L;
	public  String value;
	public  int lapIndex;
	public  int machineIndex;
	public  int uniqIndex;
	
	public learnCommand(String val ,int lapIdx,int mIndex,int uIndex)
	{
		value=val;
		lapIndex = lapIdx;
		machineIndex = mIndex;
		uniqIndex = uIndex;
		
	}
	
}
