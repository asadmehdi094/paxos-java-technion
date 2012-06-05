package paxosPckg;
import java.io.Serializable;

public abstract class Msg implements Serializable
{
	/**
	 * 
	 */
	private static final long serialVersionUID = -7857650048783363903L;
	public NodeInfo sender;
	public NodeInfo receiver;
	
}
