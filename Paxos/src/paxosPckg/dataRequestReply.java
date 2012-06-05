package paxosPckg;

public class dataRequestReply extends Msg{
		/**
	 * 
	 */
	private static final long serialVersionUID = -701784247192370917L;
		int senderindex;
		int dataindex;
		String data;
		
		public dataRequestReply(int _senderIndex,int _dataIndex,String _data)
		{
			senderindex = _senderIndex;
			dataindex = _dataIndex;
			data = _data;
		}
		
}
