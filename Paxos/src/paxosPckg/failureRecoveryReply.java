package paxosPckg;

public class failureRecoveryReply extends Msg{
	/**
	 * 
	 */
	private static final long serialVersionUID = -4602049025640130294L;
	int lastRead;
	int maxExists;
	int myIndex;
	int minExists;
	
	public failureRecoveryReply(int _lastread , int _maxExists , int _myIndex,int _minExists)
	{
		lastRead =  _lastread ;
		maxExists = _maxExists;
		myIndex = _myIndex;
		minExists = _minExists;
	}

}
