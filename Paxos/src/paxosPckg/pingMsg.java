package paxosPckg;

public class pingMsg extends Msg {

		/**
	 * 
	 */
	private static final long serialVersionUID = -1457861498792524390L;
		public int nodeIndex;
		
		public pingMsg(int index)
		{
			nodeIndex = index;
		}

}
