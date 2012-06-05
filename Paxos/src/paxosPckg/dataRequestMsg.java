package paxosPckg;

public class dataRequestMsg extends Msg{
	/**
	 * 
	 */
	private static final long serialVersionUID = -1697774100524769892L;
	int index;
	int neededData;
	
	public dataRequestMsg(int myindex , int data)
	{
		index = myindex;
		neededData = data;
	}

}
