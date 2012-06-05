package paxosPckg;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.LinkedList;
import java.util.Random;
import java.io.File;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


/*@
 * this class is in charge form group management issues , including:
 * -saving nodes info.
 * -leader election.
 * -living nodes statistics.
 * -every node role in the system.
 */
public class groupManager {
	
	private LinkedList<NodeInfo> particepents;	//the participants info
	private boolean alive[];	//alive[i] = true <-> node i is alive (a ping received from the node in the past pingTimeOut sec)
	private Date lastPing[];	//lastPing[i] the time of the last received ping from node i
	private static int pingTimeOut = 30;	//TODO haitham: i don't like this.
	private static int checkFrequency = 5;	//TODO haitham: i don't like this.
	private int myIndex;
	private int leaderIndex;
	private int numberAlive;
	private int acceptorsCount;
	private timeoutChecker TOWorker;
	private pingBroadcaster PingBC;
	
	
	private class timeoutChecker extends Thread{
		
		boolean running;
		
		public timeoutChecker(){
		//	System.out.println("timeoutChecker");
		}
		
		public void run()
		{
			running=true;
			GregorianCalendar c = new GregorianCalendar(); 
			c.setLenient(true);
			while(running)
			{

				for(int i=0;i<lastPing.length;i++)
				{
					c.setTime(lastPing[i]);
					c.add(GregorianCalendar.SECOND, pingTimeOut);
					Date tempDate = c.getTime();
					if(tempDate.before((new GregorianCalendar()).getTime()))
					{
						//30 sec or more passed from the last ping from machine i
						if(alive[i]){
							alive[i] = false;
							numberAlive--;
						}
					}else
					{
						if(!alive[i])
						{
							alive[i] = true;
							numberAlive++;
						}
					}
					
				}
				
				
				int tempLeader = getMinAliveMachine();
			//	System.out.println("tempLeader = " + tempLeader);
				if(!alive[leaderIndex] && leaderIndex!=tempLeader){
					leaderIndex=tempLeader;
					
					//broadcast the new leader to the others.

					//TODO Haitham: isn't this spam?!!!!!!
					for(int i=0;i<particepents.size();i++)
					{
						if(i!=myIndex)
						{
							newLeaderMsg tempNLMsg = new newLeaderMsg(leaderIndex);
							PaxosInstance.sendMsg(particepents.get(i).connInfo,tempNLMsg);
						}
					}

				}
				System.out.println("Leader is " + leaderIndex);
				
				try {
					Thread.sleep(checkFrequency*1000);
				} catch (InterruptedException e) {}
			}
			
			
		}
		
		public void kill()
		{
			running = false;
		}
	}
	
	private class pingBroadcaster extends Thread
	{
		private boolean running;
		public void run()
		{
			System.out.println("pingBroadcaster is running");
			running=true;
			while(running)
			{
				for(int i=0;i<particepents.size();i++)
					{
				//		System.out.println("Sending ping to " + i);
						if(i!=myIndex)
						{
							pingMsg tempPing = new pingMsg(myIndex);
							PaxosInstance.sendMsg(particepents.get(i).connInfo,tempPing);
						}else
						{
							pingLog(myIndex);
						}
					}
				
			try {
					sleep((pingTimeOut/3)*1000);
			} catch (InterruptedException e) {}
			
			}
		}
		
		public void kill()
		{
			running=false;
		}
	}
	
	public groupManager(String xmlPath ,int index) throws Exception
	{
		acceptorsCount=0;
		myIndex = index;
		particepents = new LinkedList<NodeInfo>();
		
		GregorianCalendar c = new GregorianCalendar(); 
		int participant_counter = 0;
		
		File file = new File(xmlPath);
		
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document doc = db.parse(file);
		doc.getDocumentElement().normalize();
		
		Node paxosSystem_node = doc.getElementsByTagName("paxosSystem").item(0);
		paxosSystem_node.normalize();
		NodeList machine_list = paxosSystem_node.getChildNodes();
		

		for(int i= 0; i< machine_list.getLength(); i++){
			Node machine = machine_list.item(i);
			if(machine.getNodeType() != Node.ELEMENT_NODE )
				continue;
			
			if(machine.getNodeName().equals("index")){
				myIndex = Integer.parseInt(machine.getTextContent());
			}
			if(!machine.getNodeName().equals("machine"))
					continue;
			
			participant_counter++;
			NodeList machine_details = machine.getChildNodes();
			String ip = null;
			String _port = null;
			String _acceptor = null;
			
			for(int j = 0; j< machine_details.getLength(); j++){
				Node info_node = machine_details.item(j);
				if(info_node.getNodeType() != Node.ELEMENT_NODE)
					continue;
				if(info_node.getNodeName().equals("port"))
					_port = info_node.getTextContent();
				if(info_node.getNodeName().equals("ip"))
					ip = info_node.getTextContent();
				if(info_node.getNodeName().equals("acceptor"))
					_acceptor = info_node.getTextContent();
			}
			
			int port =  Integer.parseInt(_port);
			boolean acceptor = _acceptor.equals("true");
			
			
			this.particepents.addLast(new NodeInfo(new connectionInfo(ip,port),acceptor));
			if(acceptor){
				acceptorsCount++;
			}
			
			System.out.println("IP: " + ip + " Port: " + port + " isAcceptor? " + acceptor);			
		}
		
		
		this.alive = new boolean[participant_counter];
		this.lastPing = new Date[participant_counter];
		
		
		for(int i = 0; i< participant_counter; i++){
			alive[i] =true;
			lastPing[i] = c.getTime();
		}
		

		leaderIndex = 0;
		numberAlive = particepents.size();
		
		TOWorker = new timeoutChecker();
		PingBC = new pingBroadcaster();
		TOWorker.start();
		PingBC.start();
		
		
	}
	

	private void pingLog(int machineIndex)
	{
		GregorianCalendar c = new GregorianCalendar();
		
		if(lastPing.length <= machineIndex){return;}
		lastPing[machineIndex] = c.getTime();
	}
	
	//pingMsg handler
	public void pingNotification(pingMsg m)
	{
		pingLog(m.nodeIndex);
	}
	
	//newLeaderMsg handler
	public void newLeaderNotification(newLeaderMsg m)
	{
		leaderIndex = m.leaderIndex;
	}
	
	/*
	 * getMinAliveMachine - gets The minimum index of a living machine.
	 */
	public int getMinAliveMachine()
	{
		int min = -1;
		int i=0;
		while(min == -1 && i<alive.length)
		{
			if(alive[i]){min=i;}
			i++;
		}
		return min;
	}
	
	/*
	 * getRandomLivingNode - returns a random LIVING node index , which is different from the current machine index. 
	 */
	
	public int getRandomLivingNode()
	{
		if(aliveCount() <= 1){
			return -1;
		}
		
		Random rndGenerator = new Random(System.currentTimeMillis());
		int rnd = rndGenerator.nextInt(numberAlive);
		
		if(rnd % 2 == 0){
			for(int i = rnd; i < numberAlive; i++ ){
				if(alive[i] && i != getMyIndex() )
					return i;
			}
			for(int i = rnd - 1; rnd >=0; i--){
				if(alive[i] && i != getMyIndex() )
					return i;
			}
		}
		else{
			for(int i = rnd - 1; rnd >=0; i--){
				if(alive[i] && i != getMyIndex() )
					return i;
			}
			for(int i = rnd; i < numberAlive; i++ ){
				if(alive[i] && i != getMyIndex() )
					return i;
			}			
		}
		
		return -2;		// we shouldn't reach here!
	}
	
	
	/*
	public int getRandomLivingNode()
	{
		if(aliveCount() <= 1){return -1;}
		
		Random rndGenerator = new Random(1970123);
		int rnd = rndGenerator.nextInt(numberAlive);
		int counter =0;
		for(int i=0;i<alive.length;i++)
		{
			if(alive[i]){
			if(counter ==rnd){

				if(i==getMyIndex())	
				{
					return getRandomLivingNode();
				}
			
				return i;
			
			}
			
			counter++;
			}
		}
		
		return 0;
	}
	
	*/
	
	/*
	 * returns a NodeInfo List containing all the participants in the system.
	 */
	public LinkedList<NodeInfo> getPartecipents()
	{
		return particepents;
	}
	
	/*
	 * aliveCount - returns the alive nodes count.
	 */
	public int aliveCount(){
		return numberAlive;
	}
	
	/*
	 * totalCount - returns the initial system size.
	 */
	public int totalCount(){
		return particepents.size();
	}
	
	public Date getlastPingFrom(int i)
	{
		return lastPing[i];
	}
	
	/*
	 * getLeader - returns the current leader index.
	 */
	public int getLeader(){
		return leaderIndex;
	}
	
	/*
	 * getMyIndex returns this machine index.
	 */
	public int getMyIndex(){
		return myIndex;
	}

	public NodeInfo getNodeInfo(int index)
	{
		return particepents.get(index);
	}
	
	/*
	 * acceptorsNumber - returns the LIVING acceptors number.
	 */
	public int acceptorsNumber()
	{
		int counter =0;
		for(int i=0;i<this.particepents.size();i++)
		{
			if(particepents.get(i).isAcceptor() && alive[i])
			{
				counter++;
			}
		}
		return counter;
	}
}
