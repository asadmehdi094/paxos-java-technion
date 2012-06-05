package paxosPckg;

public class newLeaderMsg extends Msg{
	/**
	 * 
	 */
	private static final long serialVersionUID = 5567969918982859063L;
	public int leaderIndex;
	
	public newLeaderMsg(int index)
	{
		leaderIndex=index;
	}

}
