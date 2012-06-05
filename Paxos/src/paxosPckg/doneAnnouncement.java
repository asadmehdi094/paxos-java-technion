package paxosPckg;

public class doneAnnouncement extends Msg{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1573395625534437626L;
	int inqueId;
	
	public doneAnnouncement(int iID)
	{
		inqueId = iID;
	}

}
