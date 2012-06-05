package paxosPckg;
import java.util.Date;


public class proposalReq extends Msg{
   /**
	 * 
	 */
	private static final long serialVersionUID = -2891499570873387798L;
	//public ballot msgIndex;
	public int sender; //sender != proposer , it is the real sender
	public int uniqIdent;
	public String value;
	public Date recvTime;
	
	public proposalReq(int senderIndex ,int uID ,String vlu)
	{
		sender = senderIndex;
		value = vlu;
		uniqIdent = uID;
	}

}
