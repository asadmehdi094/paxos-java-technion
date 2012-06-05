package paxosPckg;

public class prepareAck extends Msg{
	/**
	 * 
	 */
	private static final long serialVersionUID = -3909557618575133618L;
	public acceptReq latestAcceptedValue;
	public ballot blt;
	
	public prepareAck(acceptReq r,ballot b)
	{
		latestAcceptedValue = r;
		blt = b;
	}

}
