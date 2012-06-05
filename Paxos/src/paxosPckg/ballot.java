package paxosPckg;

import java.io.Serializable;

public class ballot implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = -6824610889781641804L;
	int machineIndex;
	int localIndex;
	
	public ballot(int mi,int li)
	{
		machineIndex = mi;
		localIndex = li;		
	}
	
	public boolean smallerThan(ballot other)
	{
		if(other == null){return false;}
		
		if(this.localIndex > other.localIndex){return false;}
		if(this.localIndex < other.localIndex){return true;}
		return (this.machineIndex <other.machineIndex);
		
	}
}
