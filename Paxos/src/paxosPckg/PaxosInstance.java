package paxosPckg;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;

/**
 * 
 * @author   Haitham Khateeb , Adi Omari
 *
 */
public class PaxosInstance {
	private Communicator commInstance;
	private groupManager groupInfo;

	private Boolean inRecoveryProcess;
	private int lastRead[]; //contains the latest read index by every other paxsos instance.
	private int currentIndex;
	private int minIndex;
	private int maxIndex; //the max index learned , should be always manindex+msgQueue-1; should be updated with the queue.
	private LinkedList<String> msgsQueue;
	private HashMap<ErrorType, Boolean> errorHash;
	private Integer recoveryNeededAcks = 0;
	
	//for recovery purpose
	
	private int learningFrom;
	private int learnUntil;
	private int learnedUntil;
	private Date lastLearned;
	private LinkedList<String> recoveryQueue =null;
	
	//proposer DS's
	private LinkedList<proposalReq> proposalsQueue;
	private boolean inProposeProcess; // to prevent proposing a new value before getting a response to the next one.
	private Integer prepareReqAcks;
	private int current_local;
	private acceptReq otherAckReq;
	private proposeProcessStarter proworker;
	
	//monitoring porpuse DS's.
	private HashMap<Integer, String> proposalsMinotors; //if there is an entry for the proposal here so it is not done yet!. (key -int uniqProposalIndex,value - String the value.).
	private Integer uniqueIndex; //need to be sych. 
	
	
	//Acceptors DS's.
	private ballot lastPromised;
	private acceptReq lastAccepted;
	private Integer acceptReqsCounter;
	//private 
	
	
	
	public enum ErrorType {
	    AcceptorFailure_place1,AcceptorFailure_place2, LeaderFailure_place1,LeaderFailure_place2,
	}
	
	/**
	 * PaxosInstance
	 * 
	 * @param errorsArray - for testing purpose  
	 * @param index - The machine index in the XML file.
	 */
	//TODO Haitham: remove index
	public PaxosInstance(ErrorType[] errorsArray,int index)
	{

		errorHash = new HashMap<ErrorType, Boolean>();
		msgsQueue = new LinkedList<String>();
		
		if(errorsArray!=null)
		{
			for(int i=0;i<errorsArray.length;i++)
			{
				errorHash.put(errorsArray[i],true);
			}
		}
		
		
		
		try {
			//TODO Haitham: remove index
			groupInfo = new groupManager("systemConf.xml",index);
		} catch (Exception e) {
			e.printStackTrace();
			// TODO write to log.
		}
		
		commInstance = new Communicator(15);
		
		//TODO: make sure it should be running here
		commInstance.start();
		
		lastRead = new int[groupInfo.totalCount()];
		maxIndex=-1;
		currentIndex=minIndex=0;
		
		for(int i=0;i<lastRead.length;i++)
		{
			lastRead[i]=0;
		}
		
		inRecoveryProcess = false;

		
		//recovery variables init.
		recoveryQueue = new LinkedList<String>();
		
		
		learningFrom = learnUntil =  learnedUntil = -1;
			

		
		
		//proposer variables init.
		 proposalsQueue = new LinkedList<proposalReq>();
		 inProposeProcess = false;  // to prevent proposing a new value before getting a response to the next one.
	     prepareReqAcks = 0;
		 current_local = 0;
	   	 otherAckReq = null;
	     proworker = new proposeProcessStarter();
	   	 proworker.start();
	   	 
	   	 
	   	//node proposals monitor init.
	   	 proposalsMinotors = new HashMap<Integer, String>();//if there is an entry for the proposal here so it is not done yet!. (key -int uniqProposalIndex,value - String the value.).
		 uniqueIndex = 0;
		 
		 
		//acceptors variables init.
		 lastPromised = null;
		 lastAccepted = null;
		 acceptReqsCounter = 0;
		 

	}

	
//===================================={LOCAL USE METHODS}==========================================
//=================================================================================================
	
	/**
	 * Returns true if this node is a leader (the leader).
	 */
	private boolean isLeader()
	{
		return (this.groupInfo.getLeader() == this.groupInfo.getMyIndex());
	}
	
/**
 * broadcasts a message to every living node.
 * 
 * @param m - message to broad cast.
 */
	public void broadCastMessage(Msg m)
	{
		LinkedList<NodeInfo> particepents =  this.groupInfo.getPartecipents();
		for(int i=0;i<particepents.size();i++)
		{	
			if(i!= groupInfo.getMyIndex()){
				signedSend(particepents.get(i).connInfo,m);
			}
		}
		
		//sends to him self
		msgHandler(m);
	}
	
	/**
	 * broadcasts a message to every living acceptor.
	 * 
	 * @param m - message to broad cast.
	 */
	public void broadCastToAcceptors(Msg m)
	{
		LinkedList<NodeInfo> particepents =  this.groupInfo.getPartecipents();
		for(int i=0;i<particepents.size();i++)
		{	
			if(i!= groupInfo.getMyIndex() && particepents.get(i).isAcceptor()){
				signedSend(particepents.get(i).connInfo,m);
			}
			
		}
		
		//sends to him self if he is not acceptor , the acceptor handler returns instantly.
		msgHandler(m);
	}
	
//=========================================={ Helping sub-classes }====================================//	
//=====================================================================================================//
	/**
	 * proposeProcessStarter
	 * 
	 * This class is a thread that runs all the time , and if the current node is a leader , it starts the proposal process.
	 */
	private class proposeProcessStarter extends Thread{
		boolean running;
		public void run()
		{
			running = true;
			while(running)
			{
				while(isLeader())
				{
					
					prepareReq preq = new prepareReq(new ballot(groupInfo.getMyIndex(),++current_local),maxIndex+1);
					prepareReqAcks=0;
					System.out.println("will send a prepare req!!");
					broadCastMessage(preq); 
					
					
					
					
					synchronized(proposalsQueue)
					{
						//TODO 
						try {proposalsQueue.wait(groupInfo.aliveCount()*800);} catch (InterruptedException e) {}
					}		
					
				
				}
				
				try {Thread.sleep(5000);} catch (InterruptedException e) {}
				
			}
		}
		
		public void kill()
		{
			running = false;
		}
	}
	
	/**
	 * ProposalMonitor
	 * 
	 * This class runs and checks if after a timeout period a certain message is not sent yet , if so , it calls a error handler with that message.
	 */
	private class ProposalMonitor extends Thread
	{
		private int uniqID;
		private userErrorHandler hndlr;
		
		public ProposalMonitor(int uID,userErrorHandler handler)
		{
			hndlr = handler;
			uniqID = uID;
		}
		
		public void run()
		{
			try {
				Thread.sleep(80000);
			} catch (InterruptedException e) {} //80 seconds timeout;
			
			if(proposalsMinotors.containsKey(uniqID))
			{
				String value = proposalsMinotors.get(uniqID);
				proposalsMinotors.remove(uniqID);
				if(hndlr!=null)
				{
					hndlr.handleError(value);
				}
			}
		}
	}
	
	/**
	 * Communicator
	 * 
	 * This node is in-charge for receiving all the incoming communication from the other nodes , and calling the appropriate handler.
	 */
	public class Communicator extends Thread
	{	
		public LinkedList<Msg> msgQueue;
		boolean running;
		private ServerSocket commSocket;
		LinkedList<deliveryWorker> workers;
		
		public Communicator()
		{
			this(15);
		}
		
		public Communicator(int workersNum)
		{
			msgQueue =  new LinkedList<Msg>();
			workers =  new LinkedList<deliveryWorker>();
			
			for(int i=0;i<workersNum;i++)
			{
				workers.add(new deliveryWorker(msgQueue));
			}

			try
			{
				commSocket = new ServerSocket(groupInfo.getNodeInfo(groupInfo.getMyIndex()).connInfo.getPort());
			}
			catch(IOException e)
			{
				//TODO: Error , write to log! 
			}
		}
		
		public void run()
		{
			System.out.println("Communicator is running");
			
			Socket socket = null;
			ObjectInputStream in;
			Msg tempMsg = null;
			running = true;
			
			for(int i=0;i<workers.size();i++)
			{
				workers.get(i).start();
			}
			
			
			while(running){
				try {
					socket = commSocket.accept();
					in = new ObjectInputStream(socket.getInputStream());
					tempMsg = (Msg)in.readObject();
				//	System.out.println("Message of type : " + tempMsg.getClass().getName() + " from " + tempMsg.sender);
			//		System.out.println("Message received");
					
				} catch (Exception e) {
					// TODO error ,write to log!
				}
				
				if(tempMsg != null){
					synchronized(msgQueue)
					{
						msgQueue.addLast(tempMsg); //FIFO
						msgQueue.notifyAll();
					}
				}
			}
			
		}
		
		public void kill()
		{
			running=false;
			for(int i=0;i<workers.size();i++)
			{
				workers.get(i).kill();
			}
		}
		
	
	}
	
	/**
	 * deliveryWorker
	 * a Helper class for Communicator.
	 */
	public class deliveryWorker extends Thread
	{
		public LinkedList<Msg> msgQueue;
		boolean running;
		
		public deliveryWorker(LinkedList<Msg> e)
		{
			msgQueue = e;
		}
		
		public void run()
		{
			running = true;
			Msg tempMsg;
			while(running)
			{
				synchronized(msgQueue)
				{
					while(msgQueue.isEmpty())
					{
						try {
							msgQueue.wait();
						} catch (InterruptedException e) {
							// TODO log error
						}
					}
					
					tempMsg = msgQueue.pollFirst(); //FIFO
				}
				msgHandler(tempMsg);
			}
		}
		


		public void kill()
		{
			running = false;
		}
	}
	
	/**
	 * learningSupervisor
	 * 
	 * This node is in-charge for monitoring the learning process after failure , if the Teacher doesn't responde for a long time it chooses a new Teacher.
	 */
	public class learningSupervisor extends Thread
	{
		boolean running;
		
		public void run()
		{
			running = true;
			GregorianCalendar c = new GregorianCalendar();
			while(running)
			{
			
				c.setTime(lastLearned);
				c.add(GregorianCalendar.SECOND, 10); //didn't get a reply for a 10 seconds!
				Date tempDate = c.getTime();
				if(tempDate.before((new GregorianCalendar()).getTime()))
				{
					if(learnedUntil < learnUntil)
					{
						learningFrom = groupInfo.getRandomLivingNode();
						//if we didn't get a reply from it for a long time that says it is a bad teacher!
						
						dataRequestMsg dataReqMsg = new dataRequestMsg(groupInfo.getMyIndex(),learnedUntil+1);
						signedSend(groupInfo.getNodeInfo(learningFrom).connInfo,dataReqMsg);
					}else
					{
						this.kill();
					}
					
				}
				
				try {Thread.sleep(5000);} catch (InterruptedException e) {}
			}
			
		}
		
		public void kill()
		{
			running = false;
		}
		
	}
	
	

//============================================{General purpose functions}===================================//
//==========================================================================================================//

	public void signedSend(connectionInfo node, Msg m)
	{
		System.out.println("signedSend:: sending message of type: " + m.getClass().getName() + " To node: " + node.getIP());
		
		m.sender = this.groupInfo.getNodeInfo(this.groupInfo.getMyIndex());
		
		connectionInfo myconn = this.groupInfo.getNodeInfo(this.groupInfo.getMyIndex()).connInfo;
		if(myconn.getIP().equals(node.getIP()) && (node.getPort()==myconn.getPort()))
		{
			msgHandler(m);
			return;
		}

		sendMsg(node, m);
	}
	
	public static void sendMsg(connectionInfo node, Msg m)
	{

		Socket socket = null;
		ObjectOutputStream out = null;
		

		
		try
		{
			socket = new Socket(node.getIP(), node.getPort());
			socket.setSoTimeout(1000);
			out = new ObjectOutputStream(socket.getOutputStream());
			out.writeObject(m);
			out.flush();
		}
		catch(Exception e)
		{
			e.printStackTrace();
			System.out.println("ERROR while sending message of type: " + m.getClass().getName());
			System.out.println("IP: " + node.getIP() + ". Port:" + node.getPort());
			//TODO: error,write to log.
		}
		finally
		{
			try
			{
				if(out != null)
					out.close();
				if(socket != null)
					socket.close();
			}
			catch(IOException e){}
		}
	}
	
	
	
//===================================={ User Interface }==============================================//
//====================================================================================================//
	
	/**
	 * readNext
	 * 
	 * Returns the next message in system.
	 */
	public String readNext()
	{
		synchronized(msgsQueue)
		{
			if(inRecoveryProcess)
			{
				if(learnedUntil < currentIndex)
				{
					return null;
				}else
				{
					currentIndex++;
					
					currentIndexUpdate cmu = new currentIndexUpdate(currentIndex,this.groupInfo.getMyIndex());
					
					this.broadCastMessage(cmu);
					
					return recoveryQueue.get((currentIndex-1)-minIndex);
				}
			}
			
			if(msgsQueue.isEmpty() || currentIndex>maxIndex){return null;}
		
			System.out.println("Current index: " + currentIndex + ". minIndex: " + minIndex + ". maxIndex: " + maxIndex);
			String rval =  msgsQueue.get(currentIndex-minIndex);
			currentIndex++;
			
			currentIndexUpdate cmu = new currentIndexUpdate(currentIndex,this.groupInfo.getMyIndex());
			
			this.broadCastMessage(cmu);
			
			return rval;
		
		}
	}
	
	public void Write(String msg)
	{
		this.Write(msg,null);
	}
	
	/**
	 * Write
	 * 
	 * @param msg - The message to add.
	 * @param hndlr - handler in case of timeout.
	 */
	public void Write(String msg,userErrorHandler hndlr)
	{	
		synchronized(uniqueIndex){
			proposalReq pReq = new proposalReq(groupInfo.getMyIndex(),++uniqueIndex,msg);
		
			signedSend(groupInfo.getNodeInfo(groupInfo.getLeader()).connInfo, pReq);
		
			proposalsMinotors.put(uniqueIndex, msg);
		
			if(uniqueIndex > 3000){uniqueIndex=0;}
		}
		
		(new ProposalMonitor(uniqueIndex,hndlr)).start(); // run a monitor on the proposal;
	}
	

	/**
	 * 
	 * @param m - this functions is a message redirector according to the type it calls the right handler.
	 */
	public void msgHandler(Msg m) {
		
		if(!(m instanceof pingMsg))
			System.out.println("msgHandler:: handling message of type: " + m.getClass().getName()  + " from " + m.sender);
		
		if(m instanceof failureRecoveryReply)
		{
			if(((failureRecoveryReply)m).lastRead > currentIndex)
			{
				currentIndex = ((failureRecoveryReply)m).lastRead;
			}
			
			synchronized(recoveryNeededAcks)
			{
				
				if(inRecoveryProcess){return;}
				recoveryNeededAcks++;
				if(((this.groupInfo.aliveCount()-1)/ 2 ) <= recoveryNeededAcks)
				{
					inRecoveryProcess = true;
					lastLearned = (new GregorianCalendar()).getTime();
					recoveryNeededAcks=0;
				}
			}
			
			recoveryReplyHandler((failureRecoveryReply)m);
			return;
		}

		if(m instanceof dataRequestReply)
		{
			dataRequestReplyHandler((dataRequestReply)m);
			return;
		}

		if(m instanceof dataRequestMsg)
		{
			dataRequestMsgHandler((dataRequestMsg)m);
			return;
		}		

		if(m instanceof currentIndexUpdate)
		{
			currentIndexHandler((currentIndexUpdate)m);
			return;
		}	

		if(m instanceof proposalReq)
		{
			proposalReqHandler((proposalReq)m);
			return;
		}	
		
		if(m instanceof prepareReq)
		{
			prepareReqHandler((prepareReq)m);
			return;
		}

		if(m instanceof prepareAck)
		{
			prepareAckHandler((prepareAck)m);
			return;
		}
		
		if(m instanceof acceptReq)
		{
			acceptReqHandler((acceptReq)m);
			return;
		}
		
		if(m instanceof learnCommand)
		{
			learnCommandHandler((learnCommand)m);
			return;
		}

		if(m instanceof indexCorrectionMsg)
		{
			IndexCorrectionHandler((indexCorrectionMsg)m);
			return;
		}		

		if(m instanceof newLeaderMsg)
		{
			newLeaderMsgHandler((newLeaderMsg)m);
			return;
		}		
		
		if(m instanceof pingMsg)
		{
			GregorianCalendar c = new GregorianCalendar(); 
			c.setLenient(true);
			c.setTime(this.groupInfo.getlastPingFrom(((pingMsg)m).nodeIndex));
			c.add(GregorianCalendar.SECOND, 60);
			
			if(c.getTime().before((new GregorianCalendar()).getTime())){
				if(!inRecoveryProcess){
				int neededindex = ((pingMsg)m).nodeIndex;
				signedSend(this.groupInfo.getNodeInfo(neededindex).connInfo, new failureRecoveryReply(lastRead[neededindex],minIndex+msgsQueue.size()-1,groupInfo.getMyIndex(),minIndex));
				}
			}
			pingMsgHandler((pingMsg)m);
			return;
		}
		
	}
	
	//===================================================================================================//
	//=========================================={ Handlers }=============================================//
	//===========This code is the real Paxos code , every thing around is only helping code==============//
	//===================================================================================================//
	//===================================================================================================//
	
	
	
	//======================================={ Recovery  }==============================================//

	
	
	public void recoveryReplyHandler(failureRecoveryReply rr)
	{
		
		synchronized(inRecoveryProcess)
		{

			
			learningFrom = rr.myIndex;

			minIndex = rr.minExists;
			learnUntil = rr.maxExists;
			maxIndex = rr.maxExists;
			
			inRecoveryProcess = true; 
			learnedUntil = minIndex-1;
			if(learnedUntil < learnUntil)
			{
				dataRequestMsg dataReqMsg = new dataRequestMsg(groupInfo.getMyIndex(),learnedUntil+1);
				signedSend(this.groupInfo.getNodeInfo(learningFrom).connInfo,dataReqMsg);
			}
			
			(new learningSupervisor()).start();
			
			
		}
	}
	
	public void dataRequestReplyHandler(dataRequestReply drr)
	{
		GregorianCalendar c = new GregorianCalendar(); 
		lastLearned = c.getTime();
		
		synchronized(recoveryQueue){
			if(learnedUntil >= drr.dataindex){return;}
			if(drr.dataindex != learnedUntil+1){return;}
			learnedUntil++;
			recoveryQueue.addLast(drr.data);
		}
		
		if(learnedUntil < learnUntil)
		{
			dataRequestMsg dataReqMsg = new dataRequestMsg(groupInfo.getMyIndex(),learnedUntil+1);
			signedSend(this.groupInfo.getNodeInfo(learningFrom).connInfo,dataReqMsg);
			
		}else
		{
			inRecoveryProcess =  false;

			synchronized(msgsQueue)
			{
				recoveryQueue.addAll(msgsQueue);
				msgsQueue = recoveryQueue;
			}
		}
	}
	
	public void dataRequestMsgHandler(dataRequestMsg drm)
	{
		int sendto = drm.index;
		int neededData = drm.neededData;
		signedSend(this.groupInfo.getNodeInfo(sendto).connInfo,new dataRequestReply(this.groupInfo.getMyIndex(),neededData,msgsQueue.get(neededData-minIndex)));
	}
	
	public void currentIndexHandler(currentIndexUpdate ciu)
	{
		
		//TODO Haitham: what if he descovers that some node knows more than him
		if(lastRead[ciu.senderIndex] < ciu.currentIndex){lastRead[ciu.senderIndex] = ciu.currentIndex;}
		int min =lastRead[0];
		for(int i=1;i<lastRead.length;i++)
		{
			if(lastRead[i]<min){min=lastRead[i];}
		}
		
		if(min > minIndex){
			
			for(int j=0;j<min-minIndex;j++)
			{
				
				this.msgsQueue.pollFirst();
			
			}
			
			minIndex=min;
			
			}
	}
	
	//======================================={ Paxos algorithm handlers }=========================================//
	
	public void proposalReqHandler(proposalReq req)
	{
		synchronized(proposalsQueue)
		{
			System.out.println("proposalReqHandler:: got new request for value: " + req.value);
			req.recvTime = (new GregorianCalendar()).getTime();
			proposalsQueue.addLast(req);
			proposalsQueue.notifyAll();
		}
	}
	
	public void prepareReqHandler(prepareReq req)
	{
		if(!groupInfo.getNodeInfo(groupInfo.getMyIndex()).isAcceptor()){return;}
		
	//	System.out.println(req.lapIndex+"-VS-"+maxIndex);
		if(req.lapIndex <= maxIndex){return;}		
		
	//	System.out.println("BLKEE HOOOOON");
		if(req.msgBallot.smallerThan(lastPromised)){
			System.out.println("prepareReqHandler:: got req with smaller ballot");
			indexCorrectionMsg cmsg = new indexCorrectionMsg(lastPromised.localIndex);
			signedSend(req.sender.connInfo ,cmsg); //TODO: make sure it is right
			return;
		}
		
		//System.out.println("LA MISH HOON");
		
		lastPromised = req.msgBallot;
		System.out.println("prepareReqHandler:: promised a new prepare request for " + req.lapIndex + ". Ballot.localIndex =  " + req.msgBallot.localIndex +  ", Ballot.machineIndex =  " + req.msgBallot.machineIndex);
		
		
	//	lastAccepted = null;
	//	prepareAck prprAck = new prepareAck(lastAccepted,req.msgBallot);
		prepareAck prprAck = new prepareAck(null,req.msgBallot);
		
		signedSend(req.sender.connInfo,prprAck);
		
	}
	
	public void prepareAckHandler(prepareAck ack)
	{
		//System.out.println("got ack for index "+ack.blt.localIndex+"");
		
		if(proposalsQueue.isEmpty()){return;} //TODO IMPORTANT check if this if is needed
		
		System.out.println("prepareAckHandler:: level 1");
		//System.out.println("---------- prepare - level 1 ----------");
		
		synchronized(prepareReqAcks)
		{
			
		
			if(ack.blt.localIndex < current_local){return;}
			
			System.out.println("prepareAckHandler:: level 2. prerpareAcks =  " + prepareReqAcks + 1);
			//System.out.println("---------- prepare - level 2 ----------");
			
			prepareReqAcks++;
			
			if(ack.latestAcceptedValue != null)
			{
				System.out.println("prepareAckHandler:: lastestAcceptedValue exists.");
				otherAckReq = ack.latestAcceptedValue;
			}
			
			if(2*prepareReqAcks > groupInfo.acceptorsNumber())
			{
				//prospoing the new value
				
				System.out.println("prepareAckHandler:: level 3.");
				//System.out.println("---------- prepare - level 3 ----------");
				
				prepareReqAcks = 0;
				acceptReq req=null;
				if(otherAckReq != null)
				{
					req = new acceptReq(otherAckReq.value, new ballot(groupInfo.getMyIndex(),current_local),maxIndex+1,otherAckReq.machineIndex,otherAckReq.uniqIndex);
					//req = new acceptReq(proposalsQueue.getFirst().value, new ballot(groupInfo.getMyIndex(),current_local),maxIndex+1,proposalsQueue.getFirst().sender,proposalsQueue.getFirst().uniqIdent);	
				}else
				{
					GregorianCalendar c = new GregorianCalendar(); 
					Date now = c.getTime();
				
					c.setLenient(true);
					c.setTime(proposalsQueue.getFirst().recvTime);
					c.add(GregorianCalendar.SECOND, 40);
					
					while(c.getTime().before(now))
					{
						proposalsQueue.pollFirst();
						c.setTime(proposalsQueue.getFirst().recvTime);
						c.add(GregorianCalendar.SECOND, 40);
					}
					
					req = new acceptReq(proposalsQueue.getFirst().value, new ballot(groupInfo.getMyIndex(),current_local),maxIndex+1,proposalsQueue.getFirst().sender,proposalsQueue.getFirst().uniqIdent);	
				}
			
				System.out.println("prepareAckHandler:: before send accept with: " + req.value);
				broadCastToAcceptors(req);
				System.out.println("prepareAckHandler:: send accept with: " + req.value);
			}
		
		}
	}
	
	
	public void acceptReqHandler(acceptReq newReq)
	{
		System.out.println("acceptReqHandler:: IN");
		
		System.out.println("acceptReqHandler:: newReq.blt.machineIndex: " + newReq.blt.machineIndex + ", newReq.blt.localIndex" + newReq.blt.localIndex);
		
//		if((lastAccepted != null) && !lastAccepted.blt.smallerThan(newReq.blt) && !(lastAccepted.blt.localIndex==newReq.blt.localIndex && lastAccepted.blt.machineIndex==newReq.blt.machineIndex))
//		{
//			System.out.println("acceptReqHandler:: already accepted greater ballot. old ballot = (" + lastAccepted.blt.machineIndex + ", " + lastAccepted.blt.localIndex + ") new ballot = (" + newReq.blt.machineIndex + ", " + newReq.blt.localIndex + ")");
//			return;
//		}
		
		
		
		//System.out.println("----------[Acceptor] - level 1 passed----------");
		
		if(newReq.blt.smallerThan(lastPromised)){
			System.out.println("acceptReqHandler:: already promised grater ballot");
			return;
		}
		
	//	System.out.println("----------[Acceptor] - level 2 passed----------");
		
		System.out.println("acceptReqHandler:: promise passed.");
		
		synchronized(acceptReqsCounter)
		{
		
			if((lastAccepted!=null)&&(lastAccepted.blt.localIndex==newReq.blt.localIndex && lastAccepted.blt.machineIndex==newReq.blt.machineIndex))
			{
				acceptReqsCounter++;
				System.out.println("acceptReqHandler::  level 2.5 passed => counter:"+acceptReqsCounter);
			//	System.out.println("----------[Acceptor] - level 2.5 passed => counter:"+acceptReqsCounter+"----------");
				if(2*acceptReqsCounter > groupInfo.acceptorsNumber())
				{
					System.out.println("acceptReqHandler:: new value should be learned: " + lastAccepted.value);
					
				//	System.out.println("----------[Acceptor] - level 3 passed----------");
					learnCommand lrnCmnd = new learnCommand(lastAccepted.value, lastAccepted.lapIndex, lastAccepted.machineIndex, lastAccepted.uniqIndex);
					broadCastMessage(lrnCmnd);					
				//	acceptReqsCounter = 0;
				}
				
			}
			else
			{
				System.out.println("acceptReqHandler:: new accept handling started. lastAccepted was " + lastAccepted);
				
				acceptReqsCounter = 1;
				lastAccepted = newReq;
				broadCastToAcceptors(newReq);
			}
			
			
			System.out.println("acceptReqHandler:: acceptReqsCounter = " + acceptReqsCounter);
		//	System.out.println("----------[Acceptor] - counter = "+acceptReqsCounter+"----------");
		
		}
	}
	
	
	/*
	
	public void learnCommandHandler(learnCommand lrnCmnd)
	{
			
		System.out.println("learnCommandHandler:: learning new value = " + lrnCmnd.value);
		
		if(this.isLeader())
		{
			synchronized(proposalsQueue){
				proposalReq  pReq = this.proposalsQueue.getFirst();
				System.out.println("learnCommandHandler:: pReq.uniqIdent = " + pReq.uniqIdent + ". lrnCmnd.uniqIndex = " + lrnCmnd.uniqIndex);
				if((pReq.sender == lrnCmnd.machineIndex)&&(pReq.uniqIdent == lrnCmnd.uniqIndex))
				{
					this.proposalsQueue.pollFirst();
					otherAckReq = null;
					 current_local=0;
					maxIndex++;
					proposalsQueue.notifyAll();
				}
			}
			
		}
		
		synchronized(msgsQueue)
		{
			if(lrnCmnd.lapIndex <= maxIndex){return;}
			
			msgsQueue.addLast(lrnCmnd.value);
			if(!this.isLeader())
			{
				maxIndex++;
			}
		}
		
		//if it my porposal
		if(lrnCmnd.machineIndex == this.groupInfo.getMyIndex())
		{
			this.proposalsMinotors.remove(lrnCmnd.uniqIndex);
		}
		
		//accepor init.
		lastPromised = null;
		lastAccepted = null;
		otherAckReq = null;
		acceptReqsCounter = 0;

	}
	
	*/
	
	
	
	
	public void learnCommandHandler(learnCommand lrnCmnd)
	{
			
		System.out.println("learnCommandHandler:: learning new value = " + lrnCmnd.value);
		
		synchronized(msgsQueue)
		{
			if(lrnCmnd.lapIndex <= maxIndex){return;}
			
			//accepor init.
			lastPromised = null;
			lastAccepted = null;
			otherAckReq = null;
			acceptReqsCounter = 0;
			
			msgsQueue.addLast(lrnCmnd.value);
			maxIndex++;
			
			System.out.println("learnCommandHandler:: added the queue a value = " + lrnCmnd.value);
		}
		
		//if it my porposal
		if(lrnCmnd.machineIndex == this.groupInfo.getMyIndex())
		{
			this.proposalsMinotors.remove(lrnCmnd.uniqIndex);
		}
		
		

		
		
		if(this.isLeader())
		{
			synchronized(proposalsQueue){
				proposalReq  pReq = this.proposalsQueue.getFirst();
				if((pReq.sender == lrnCmnd.machineIndex)&&(pReq.uniqIdent == lrnCmnd.uniqIndex))
				{
					this.proposalsQueue.pollFirst();	
					System.out.println("learnCommandHandler:: removed propsalreq from proposalsQueue");
				}
				
				otherAckReq = null;		
				current_local=0;
				proposalsQueue.notifyAll();
			}
			
		}

		
	}
	
	
	public void IndexCorrectionHandler(indexCorrectionMsg m)
	{
		if(m.index > current_local)
		{
			current_local = m.index;
		}
	}
	
	public void newLeaderMsgHandler(newLeaderMsg msg)
	{
		this.groupInfo.newLeaderNotification(msg);
	}
	
	
	public void pingMsgHandler(pingMsg pg)
	{
		this.groupInfo.pingNotification(pg);
	}
	
	
}
