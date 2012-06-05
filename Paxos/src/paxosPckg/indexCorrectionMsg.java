package paxosPckg;

class indexCorrectionMsg extends Msg {
	/**
	 * 
	 */
	private static final long serialVersionUID = -4962820033190894460L;
	public int index;
	
	public indexCorrectionMsg(int ind){
		index = ind;
	}

}
