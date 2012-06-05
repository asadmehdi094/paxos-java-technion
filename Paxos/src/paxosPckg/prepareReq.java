package paxosPckg;

public class prepareReq extends Msg{
	/**
	 * 
	 */
	private static final long serialVersionUID = 3651209946516993136L;
	public ballot msgBallot;
	public int lapIndex;
	
	public prepareReq(ballot blt,int lap)
	{
		msgBallot = blt;
		lapIndex = lap;
	}

}
