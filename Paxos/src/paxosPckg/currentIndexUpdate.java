package paxosPckg;

public class currentIndexUpdate extends Msg{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -3417627871791397557L;
	public int currentIndex;
	public int senderIndex;
	
	public currentIndexUpdate(int cmu , int sendInd)
	{
		currentIndex = cmu;
		senderIndex = sendInd;
	}

}
