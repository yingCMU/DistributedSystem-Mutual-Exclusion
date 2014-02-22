package lab;



import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;

import static java.nio.file.StandardWatchEventKinds.*;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.JTextArea;

import org.yaml.snakeyaml.TypeDescription;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

/*
 * Message
 * Author: Ying Li (qianm)
 *         Utsav Drolia (udrolia)
 * Date: 1/18/2014
 */

public class MessagePasser {
    // static variables    
    private String configuration_filename = null;
    private File confFile = null;
    private String local_name = null;
    private Path confPath = null;
    private WatchService watcher = null;
    private ClockService clockService = null;
    LockState state = LockState.RELEASED;
    boolean voted = false;
    ArrayList<String> myGroupNames = null;
    HashMap<String,HashSet<String>> voteLists = new HashMap<String,HashSet<String>>();// track ack
	
    private ConcurrentLinkedQueue<TimeStampedMessage> deliveryBuffer = new ConcurrentLinkedQueue<TimeStampedMessage>();
    // for mutual exclusion requests
    LinkedList<Request> requestQueue = new LinkedList<Request>();
 Deque<TimeStampedMessage> causalHoldbackQueue = new LinkedList<TimeStampedMessage>();

    private Deque<OutgoingMessage> delaySendBuffer = new LinkedList<OutgoingMessage>();
    
    private HashMap<String, HashMap<String,AtomicInteger>>  recvSeqTracker=new HashMap<String, HashMap<String,AtomicInteger>>();
    private HashMap<String, Integer> groupSendSeqTracker = new HashMap<String, Integer>(); 
	//for multicast msg that is out of order, put them temporily here 
	private HashMap<String, HashMap<Integer,TimeStampedMessage>> sentMessageCache = new HashMap<String, HashMap<Integer,TimeStampedMessage>>(); 
	private HashMap<String, HashMap<String, HashMap<Integer,TimeStampedMessage>>> recvMessageCache = new HashMap<String, HashMap<String, HashMap<Integer,TimeStampedMessage>>>(); 
    private static final int CacheSize = 50;
    private HashMap<String, ClockService> multiVectorClockMap = new HashMap<String, ClockService>();
    //private HashMap<String, List<String>> groupMap;

    private int currentSeqNum = -1;
    private Configuration conf = null;
    private TCPNetCommThreaded comm = null;
	private JTextArea statusArea;
    public void releaseLock(){
    	System.out.println("!!!!!!!! "+local_name+" : releasing lock");
    	MultiCastMessage release = new MultiCastMessage( "this is release msg ");
    	release.setLockType(LockType.RELEASE);
		multiSend(release,myGroupNames.get(0));
		state = LockState.RELEASED;
    }
    public void requestLock(){
    	System.out.println(local_name+" : requesting lock");
    	state = LockState.WANTED;
    	MultiCastMessage request = new MultiCastMessage( "this is request msg ");
    	request.setLockType(LockType.REQUEST);
    	request.setKind("request");
    	multiSend(request,myGroupNames.get(0));
    }
    
    public MessagePasser(JTextArea statusArea, String configuration_filename, String local_name, ClockService clockService) throws Exception {
        this.statusArea = statusArea;
    	this.configuration_filename = configuration_filename;
        this.local_name = local_name;
        this.clockService = clockService;
        
        // parse the configuration file
        configurationParse();
        
        
        init();
        
        // register watcher for configuration file
        confFile = new File(configuration_filename);
        confPath = Paths.get(confFile.getParentFile().getPath());
        watcher = FileSystems.getDefault().newWatchService();
        try {
            WatchKey key = confPath.register(watcher, ENTRY_MODIFY);
        } catch (IOException x) {
            System.err.println(x);
        }
        
        // configure sockets
        if (!conf.hostMap.containsKey(local_name)) {
            throw new Exception("local name doesn't match configuration");
        } else {
            comm = new TCPNetCommThreaded(conf.hostMap.get(local_name).port);
        }
        
        // start background carrier thread
        (new Thread(new CarrierThread(comm))).start();

    }
    


    
    // "destructor"
    protected void finalize( ) throws Throwable {
        comm.NetClose();
    }
    
    // helper function to parse the configuration file
    private void configurationParse() {
        try {
            // read file
            BufferedReader in = getFileByPath(configuration_filename);
            
            // parse YAML
            Constructor constructor = new Constructor(Configuration.class);
            TypeDescription carDescription = new TypeDescription(Configuration.class);
            carDescription.putListPropertyType("configuration", Configuration.Host.class);
            carDescription.putListPropertyType("sendRules", Configuration.Rule.class);
            carDescription.putListPropertyType("receiveRules", Configuration.Rule.class);
            carDescription.putListPropertyType("groups", Configuration.Group.class);
            constructor.addTypeDescription(carDescription);
            Yaml yamlParser = new Yaml(constructor);
            
            conf = (Configuration) yamlParser.load(in);
            conf.initialize();
            myGroupNames = (ArrayList<String>) conf.hostMap.get(local_name).memberOf;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    // helper function to build a buffer reader from URL
    private BufferedReader getFileByURL(String configuration_filename) throws IOException {
        URL url = new URL(configuration_filename);
        return new BufferedReader(new InputStreamReader(url.openStream()));
    }

    // helper function to build a buffer reader from local file path
    private BufferedReader getFileByPath(String configuration_filename) throws FileNotFoundException {
        return new BufferedReader(new FileReader(configuration_filename));
    }
    
    // check if the configuration file has been modified and reload
    private void checkReloadConf() {
        WatchKey wk;
        wk = watcher.poll();
        
        if (wk == null) {
            return;
        }
        
        for (WatchEvent<?> event : wk.pollEvents()) {
            //we only register "ENTRY_MODIFY" so the context is always a Path.
            final Path changed = (Path) event.context();
            System.out.println(changed);
            if (configuration_filename.endsWith(changed.getFileName().toString())) {
                // reload configuration
                configurationParse();
            }
        }
        // reset the key
        wk.reset();
    }
    
    private void init(){
    	
    	for(Entry<String, List<String>> entry : conf.groupMap.entrySet())
    	{
    		voteLists.put(entry.getKey(), new HashSet<String>());
    		groupSendSeqTracker.put(entry.getKey(), 0);
    		multiVectorClockMap.put(entry.getKey(), ClockFactory.getClockService(Clock.VECTOR, local_name, entry.getValue()));
    		sentMessageCache.put(entry.getKey(), new HashMap<Integer,TimeStampedMessage>() );
    		recvSeqTracker.put(entry.getKey(), new HashMap<String,AtomicInteger>());
    		recvMessageCache.put(entry.getKey(), new HashMap<String, HashMap<Integer,TimeStampedMessage>>());
    		for(String member : entry.getValue())
    		{
    			recvSeqTracker.get(entry.getKey()).put(member, new AtomicInteger(0) ) ;
    			recvMessageCache.get(entry.getKey()).put(member, new HashMap<Integer,TimeStampedMessage>());
    		}
    	}
    }
    
 	//Send Receive methods ---------------------------------------------
    
    //unicast send
    public void send(TimeStampedMessage message) throws UnknownHostException {
        // check if message is null or unreachable
        if (message == null || !conf.hostMap.containsKey(message.getDest())) {
            return;
        }
        
        // check and update configuration file
        checkReloadConf();
        
        // set Seq and Src
        this.currentSeqNum++;
        message.set_seqNum(this.currentSeqNum);
        message.set_source(this.local_name);
        
        //Update clock on Send
        clockService.updateClockOnSend();
        
        //Add timestamp
        message.setTimeStamp(clockService.getCurrentTimeStamp());
        
        InternalSend(message);
    }
    
    
    public void multiSend(TimeStampedMessage message, String groupID) throws IllegalArgumentException
    {   
    	List<String> groupList = conf.groupMap.get(groupID);
    	Iterator<String> it = groupList.iterator();
    	if (message == null ) {
            return;
        }
        
        // check and update configuration file
        checkReloadConf();
        
        // set Seq and Src
        this.currentSeqNum++;
        message.set_seqNum(this.currentSeqNum);
        message.set_source(this.local_name);
    	//Check if node is part of group
    	if(!groupList.contains(local_name))
    		throw new IllegalArgumentException("Not Part of Group");
    	
    	//Increment sendSeqNumber for this group
    	groupSendSeqTracker.put(groupID, groupSendSeqTracker.get(groupID) + 1 );
    	
    	//Update This group's clock
    	multiVectorClockMap.get(groupID).updateClockOnSend();
    	
    	//Add Clock to message
    	message.setTimeStamp(multiVectorClockMap.get(groupID).getCurrentTimeStamp());

    	
    	//Send MultiCastMessage to each node in group including yourself
    	while(it.hasNext())
    	{
    		message.setDest(it.next());
    		MultiCastMessage M = new MultiCastMessage(message);
    		//System.out.println("## in mutlsend: M \t"+M.toString());
    		M.setGroupID(groupID);
    		M.setACKs(new HashMap<String,AtomicInteger>(recvSeqTracker.get(groupID)));
    		M.setMessageType(MessageType.MULTICAST);
    		M.setControlType(ControlType.NORMAL);
    		M.setMulticastSeq(groupSendSeqTracker.get(groupID).intValue());
    		try {
				InternalSend(M);
				System.out.println(M.toString());
			} catch (UnknownHostException e) 
			{
				System.out.println("Remote Peer Unknown");
				e.printStackTrace();
			}
    	}
    	
    	message.setDest("none");
		MultiCastMessage M = new MultiCastMessage(message);
		M.setGroupID(groupID);
		M.setACKs(recvSeqTracker.get(groupID));
    	sentMessageCache.get(groupID).put(groupSendSeqTracker.get(groupID) % CacheSize, M);
    }
    
    
    //Internal Send
    private void InternalSend(TimeStampedMessage message) throws UnknownHostException
    {
        
        Configuration.Host dest = conf.hostMap.get(message.getDest());
        
        // check send rules
        if (conf.sendRules != null) {
            for (Configuration.Rule rule : conf.sendRules) {
                if (rule.match(message)) {
                    // perform action
                    if (rule.action.equals(Configuration.Action.DROP.getAction())) {
                        // ignore the message
                        return;
                    } else if (rule.action.equals(Configuration.Action.DUPLICATE.getAction())) {
                        // duplicate the message (send two identical message and set dupe for second message)
                    	// Utsav - We can do this by storing both messages in the sendBuffer 
                        InetAddress ip = InetAddress.getByName(dest.ip);
                        int port = dest.port;
                        comm.NetSend(new MultiCastMessage(message), ip, port);
                        
                        // send all delayed message in buffer
                        sendAllDelayedMsg();
                        
                        TimeStampedMessage dupMsg = new MultiCastMessage(message);
                        dupMsg.set_duplicate(new Boolean(true));
                        comm.NetSend(dupMsg, ip, port);
                        return;
                    } else if (rule.action.equals(Configuration.Action.DELAY.getAction())) {
                        // store it in delaysendBuffer
                    	// Utsav - After sending the normal messages, check if there is anything in the
                    	// delaysendBuffer. If yes, send that.
                    	delaySendBuffer.offer(new OutgoingMessage(new MultiCastMessage(message), InetAddress.getByName(dest.ip), dest.port));
                        return;
                    }
                }
            }
        }

        // no match for any send rules, send normal message
        //System.out.println(message.toString());
        comm.NetSend(message, InetAddress.getByName(dest.ip), dest.port);
        
        // send delayed message in buffer
        sendAllDelayedMsg();
        return;
    }
    
    // intermediate object to hold message, ip and port
    private class OutgoingMessage {
        public TimeStampedMessage M;
        public InetAddress destAddress;
        public int port;
        
        public OutgoingMessage(TimeStampedMessage m, InetAddress destAddress, int port) {
            super();
            M = m;
            this.destAddress = destAddress;
            this.port = port;
        }
    }
    
    // helper function to send all delayed messages
    private void sendAllDelayedMsg() {
        OutgoingMessage delayedMsg = null;
        while (delaySendBuffer.peek() != null) {
            delayedMsg = delaySendBuffer.poll();
            System.out.println(delayedMsg.M.getTimeStamp().toString());
            comm.NetSend(delayedMsg.M, delayedMsg.destAddress, delayedMsg.port);
        }
    }

    // non-block receive  
    TimeStampedMessage receive() throws IllegalArgumentException{
        // get the first message from receive buffer and return
        if (deliveryBuffer.peek() == null) {
            return null;
        } else {
        	//Update clock on message receive
        	TimeStampedMessage tsm = deliveryBuffer.poll();
        	if(tsm.getTimeStamp().getClockType() != clockService.getClockType())
        		throw new IllegalArgumentException();
        	//clockService.updateClockOnReceive(tsm.getTimeStamp());
        	System.out.println(tsm.getTimeStamp().toString());
            return tsm;
        }
    }
    
    // helper function to receive all delayed messages (send them into receive queue for application)
    
    // thread to keep receiving from low level side and applying rules
    private class CarrierThread implements Runnable {
        private TCPNetCommThreaded comm = null;
    	private LinkedList<MultiCastMessage> recvHoldBackQueue = new LinkedList<MultiCastMessage>();
    	private LinkedList<MultiCastMessage> recvQueue = new LinkedList<MultiCastMessage>();
    	private LinkedList<TimeStampedMessage> receiveBuffer = new LinkedList<TimeStampedMessage>();
        private Deque<TimeStampedMessage> delayReceiveBuffer = new LinkedList<TimeStampedMessage>();

        // constructor
        public CarrierThread(TCPNetCommThreaded comm) {
            this.comm = comm;
        }
        
        private void causalOrder()
        {	
        	System.out.println("!! causalorder checking ");
	        	
        	MultiCastMessage msg = null;
        	while((msg = recvQueue.peek())!=null){
        		//System.out.println("##msg not null: "+msg.toString());
        		if(msg.getLockType()==LockType.REQUEST){
        			System.out.println("## receive request from "+msg.getSource());
        			 String displayMsg = String.format("%s: receive request from %s\n", local_name,msg.getSource());
	                    UIThread.textArea.append(displayMsg);
					
        			if(state == LockState.HELD || voted == true){
        				System.out.println("##held or voted, adding request to queue+"+msg.getRequestTS());
        				 displayMsg = String.format("%s: held or voted, adding request to queue\n", local_name);
	                    UIThread.statusArea.append(displayMsg);
        				Request e = new Request(msg.getRequestTS().getTime(),msg.getSource());
        				requestQueue.add(e);
        			}
        			else{//vote for it
        				TimeStampedMessage vote = new TimeStampedMessage(msg.getSource(), null, "this is vote msg from "+local_name, null);
        				vote.setLockType(LockType.VOTE);
        				System.out.println("## voting for "+msg.getSource());
        				 displayMsg = String.format("%s: voting for %s\n", local_name,msg.getSource());
 	                    UIThread.textArea.append(displayMsg);
 	                   displayMsg = String.format("%s: voted for %s\n", local_name,msg.getSource());
	                    
 	                   UIThread.statusArea.append(displayMsg);
         				
        				try {
							send(vote);
						} catch (UnknownHostException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
        				voted = true;
        			}
        			recvQueue.poll();
        		}
        		else if(msg.getLockType()==LockType.RELEASE){
        			System.out.println("## release received from "+msg.getSource());
        			 String displayMsg = String.format("%s: vrelease received from  %s\n", local_name,msg.getSource());
	                    UIThread.textArea.append(displayMsg);
	                    displayMsg = String.format("%s: release  from %s\n", local_name,msg.getSource());
	                    
	 	                   UIThread.statusArea.append(displayMsg);
      				if(!requestQueue.isEmpty()){
        				System.out.println("## request queue not empty");
        				timeSort(requestQueue);
    					Request head = requestQueue.poll();
    					TimeStampedMessage vote = new TimeStampedMessage(head.name, null, "this is vote msg from "+local_name, null);
    					vote.setLockType(LockType.VOTE);
    					System.out.println("## voting for "+head.name);
    					displayMsg = String.format("%s: voted for  %s\n", local_name,head.name);
	                    UIThread.statusArea.append(displayMsg);
	                    UIThread.textArea.append(displayMsg);
    					try {
							send(vote);
						} catch (UnknownHostException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
    					voted = true;
    				
    				}
    				else{
    					System.out.println("## request queue empty, not voting");
    					voted = false;
    					state = LockState.RELEASED;
    				}
        			recvQueue.poll();
        		}
        		else
        			deliveryBuffer.offer(recvQueue.poll());
        	}
        	
        }
        
        private void timeSort(LinkedList<Request> requestQueue) {
			// TODO Auto-generated method stub
        	Collections.sort(requestQueue);
    		System.out.println("sorting request\n"+requestQueue);
		}

		private void receiveAllDelayedMsg() {
            TimeStampedMessage message;
			while ((message=delayReceiveBuffer.peek()) != null) {
            	if(message.getLockType()==LockType.VOTE)
            		recvVote(message,myGroupNames.get(0));
            	else
                	receiveBuffer.offer(delayReceiveBuffer.poll());
            }
        }

        public void run() 
        {
        	
            boolean isNormal = true;

            while (true) {
                // check if configuration has changed and reload
                checkReloadConf();
                //get a message from netComm
                isNormal = true;
                TimeStampedMessage message = null;
                TimeStampedMessage tempM = comm.NetReceive();
	            if(tempM != null)
                	if(tempM instanceof MultiCastMessage){
                		message = new MultiCastMessage(tempM);
                		TimeStamp ts = clockService.getCurrentTimeStamp();
	                	((MultiCastMessage)message).setRequestTS((LogicalTimeStamp) ts);
	                	clockService.updateClockOnReceive(ts);
	                	System.out.println("---rev: Multicast setting ts:"+((MultiCastMessage)message).getRequestTS().getTime());
                		
                	}
	            
	                else
	                	message = new TimeStampedMessage(tempM);
                
                

                // push it into receive buffer
                if (message != null) {
                    // check receive rules
                    if (conf.receiveRules != null) {
                        for (Configuration.Rule rule : conf.receiveRules) {
                            if (rule.match(message)) {
                                // perform action
                                if (rule.action.equals(Configuration.Action.DROP.getAction())) {
                                    // ignore the message
                                    isNormal = false;
                                    break;
                                } else if (rule.action.equals(Configuration.Action.DUPLICATE.getAction())) {
                                    // duplicate the message (send two identical message to application
                                    receiveBuffer.offer(message);
                                    
                                    // push all delayed messages into buffer
                                    receiveAllDelayedMsg();
                                    receiveBuffer.offer(message);
                                    isNormal = false;
                                    break;
                                } else if (rule.action.equals(Configuration.Action.DELAY.getAction())) {
                                    // store it in delayreceiveBuffer
                                	System.out.println("--DELAY: \n"+message.toString());
                                    delayReceiveBuffer.offer(message);
                                    isNormal = false;
                                    break;
                                }
                            }
                        }
                    }
                    // normal message
                    if (isNormal)  
                    {
	                        receiveBuffer.offer(message);
	                        recvVote(message,myGroupNames.get(0));
	                        // push all delayed message into buffer
	                        receiveAllDelayedMsg();
                    	
                    }
                }
                
                //Check receive buffer to process messages
                if (!receiveBuffer.isEmpty())
                {
                	
                	TimeStampedMessage tempMsg = receiveBuffer.poll();
                	System.out.println(tempMsg.toString());
                	if(tempMsg instanceof MultiCastMessage)
                		//processMulticast(new MultiCastMessage(tempMsg));
                		{
                		//System.out.println("## enterning processMulticas +"+tempMsg.toString());
                		processMulticast((MultiCastMessage) tempMsg);
                }
                	else
                		deliveryBuffer.offer(new TimeStampedMessage(tempMsg));
                	//receiveBuffer.poll();
                }
                	
                
                try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
            }
        }
        
       /*for mutual exclutsion, check release*/
        private boolean recvVote(Message msg,String groupID){
        
        	if(msg.getLockType()==LockType.VOTE){
        		System.out.println("recved VOTE from "+msg.getSource());
        		 String displayMsg = String.format("%s: recved VOTE from %s\n", local_name,msg.getSource());
                 UIThread.textArea.append(displayMsg);
				
        		synchronized (voteLists) {
        			System.out.println(voteLists.get(groupID));
        			System.out.println(msg.getSource());
    				voteLists.get(groupID).add(msg.getSource());
    				if(voteLists.get(groupID).size()==conf.groupMap.get(groupID).size()){
    					System.out.println("!!!!!!!!!!!!!!!!!! "+local_name+" is entering CS");
    					  
    					state=LockState.HELD;
    					displayMsg = String.format("%s: entering CS after got every votes\n", local_name);
    	                    UIThread.statusArea.append(displayMsg);
    					return true;
    				}
    				System.out.println("!!!!!!blocked, waiting for vote ");
    				displayMsg = String.format("blocked,still waiting for vote\n");
                    UIThread.statusArea.append(displayMsg);
    			}
        		
        	}
        	return false;
        	
        }
        private boolean checkACKs(MultiCastMessage msg)
        {
        	
        	String groupID = msg.getGroupID();
        	boolean nackFlag = true;
        	
        	for (Entry<String, AtomicInteger> ent : msg.getACKs().entrySet())
	       	{
	       		String sender=ent.getKey();
	       		int senderSeq = ent.getValue().get();
        		AtomicInteger lastSeqNum =  recvSeqTracker.get(groupID).get(sender);

	       		if(senderSeq > lastSeqNum.get())
	       		{
	       			 
	       			System.out.println("multicast missing, from sender:" + sender +" seq->"+senderSeq);
	       			// some previous msg missing, 
			   		
		       		
		       	// set Seq and Src
		       		TimeStampedMessage message = new TimeStampedMessage(" ", " ", new NACKRequest(sender,lastSeqNum.get()+1,senderSeq), null);
		            message.set_seqNum(currentSeqNum);
		            message.set_source(local_name);
		        	
		            List<String> groupList = conf.groupMap.get(groupID);
		        	Iterator<String> it = groupList.iterator();
		        	
		        	//Add Clock to message
		        	message.setTimeStamp(multiVectorClockMap.get(groupID).getCurrentTimeStamp());

		        	
		        	//Send MultiCastMessage to each node in group including yourself
		        	while(it.hasNext())
		        	{
		        		message.setDest(it.next());
		        		MultiCastMessage M = new MultiCastMessage(message);
		        		M.setGroupID(groupID);
		        		M.setACKs(new HashMap<String,AtomicInteger>(recvSeqTracker.get(groupID)));
		        		M.setMessageType(MessageType.MULTICAST);
		        		M.setControlType(ControlType.NACK);
		        		M.setMulticastSeq(groupSendSeqTracker.get(groupID).intValue());
		        		try {
		    				InternalSend(M);
		    				System.out.println(M.toString());
		    			} catch (UnknownHostException e) 
		    			{
		    				System.out.println("Remote Peer Unknown");
		    				e.printStackTrace();
		    			}
		        	}
			   		
			   		// in receiver thread, when you recv NACK  , then what?? to do
			   		nackFlag = false;
		       	
		       	}
       		
	       	}
	       	return nackFlag;
       
        }
       /*
        * for multicasting comminication, check if reliable and can be delivered
        * if valid, return true
        * else return false and send NACK
        */
        public void processMulticast(MultiCastMessage msg)
        {
	       	System.out.println("!! Multicast Processing:" );
        	switch (msg.getControlType()) 
	       	{
		       	case NORMAL:
		       	{	
		       		System.out.println("case normal");
		   	    	String sender = msg.getSource();
		   	    	int senderSeq = msg.getMulticastSeq();
		   	    	String groupID = msg.getGroupID();
		   	    	if( checkOne(sender, msg, senderSeq) == 1 && checkACKs(msg) == true)
		   	    	{
		   	    		//Put msg in recvQueue for causal checking
		   	    		recvQueue.offer(new MultiCastMessage(msg));
		   	    		//Put msg in recvCache
		   	    		recvMessageCache.get(groupID).get(sender).put(senderSeq % CacheSize, new MultiCastMessage(msg));
			       		System.out.println("multicast in order, seq->"+senderSeq);
			       		recvSeqTracker.get(groupID).put(sender,new AtomicInteger(senderSeq) );
			       		
			       		checkHoldBackQueue(groupID, sender);
			       		
			       		causalOrder();
		   	    	}
		   	    	else if(checkOne(sender, msg, senderSeq) == 2 || checkACKs(msg) == false)
		   	    	{
		   	    		System.out.println("msg out of order based on sender seq");
		   	    		recvHoldBackQueue.offer(new MultiCastMessage(msg));
		   	    	}
		   	    	break;
		       			
		       	}
		       	
		       	case NACK:
		       		
		       		NACKRequest nackReq = (NACKRequest) msg.data;
		       		String requester = msg.getSource();
		       		String groupID = msg.getGroupID();
		       		String source = nackReq.getSource();
		       		
		       		System.out.println("NACK recved, resending to "+msg.getSource());
		       		for(Integer seq=nackReq.getFirstSeqNum(); seq <= nackReq.getLastSeqNum() ; seq++ )
		       		{
		       			MultiCastMessage tsM = (MultiCastMessage)recvMessageCache.get(groupID).get(source).get(seq);
		       			if(tsM != null)
		       			{
			       			MultiCastMessage reply = new MultiCastMessage(tsM);
			       			
			       			reply.setDest(requester);
			       			
			       			try {
								InternalSend(reply);
							} catch (UnknownHostException e) {
								e.printStackTrace();
							}
		       			}
		       			
		       		}
		       		
		       	   // to do, where to find the sent multicast msg??
		       		break;
		       		
		       	default:
		       		break;
	        }
	       	
        }
        
        
        
        private int checkOne(String sender,MultiCastMessage msg, int senderSeq)
        {
        	
        	AtomicInteger lastSeqNum =  recvSeqTracker.get(msg.getGroupID()).get(sender);
        	String groupID = msg.getGroupID();
        	if(senderSeq == lastSeqNum.get()+1)
        	{	
	       		return 1;
       		}
        	
	       	else if(senderSeq > lastSeqNum.get()+1)
	       	{
	       		System.out.println("multicast missing, seq->"+senderSeq);
	       		// some previous msg missing, 
	       		
	       	// set Seq and Src
	       		MultiCastMessage message = new MultiCastMessage(" ", " ", new NACKRequest(sender,lastSeqNum.get()+1,senderSeq-1), null);
	            message.set_seqNum(currentSeqNum);
	            message.set_source(local_name);
	        	
	            List<String> groupList = conf.groupMap.get(groupID);
	        	Iterator<String> it = groupList.iterator();
	        	
	        	//Add Clock to message
	        	message.setTimeStamp(multiVectorClockMap.get(groupID).getCurrentTimeStamp());

	        	
	        	//Send MultiCastMessage to each node in group including yourself
	        	while(it.hasNext())
	        	{
	        		message.setDest(it.next());
	        		MultiCastMessage M = new MultiCastMessage(message);
	        		M.setGroupID(groupID);
	        		M.setACKs(new HashMap<String,AtomicInteger>(recvSeqTracker.get(groupID)));
	        		M.setMessageType(MessageType.MULTICAST);
	        		M.setControlType(ControlType.NACK);
	        		M.setMulticastSeq(groupSendSeqTracker.get(groupID).intValue());
	        		try {
	    				InternalSend(M);
	    				System.out.println(M.toString());
	    			} catch (UnknownHostException e) 
	    			{
	    				System.out.println("Remote Peer Unknown");
	    				e.printStackTrace();
	    			}
	        	}
	        	
	       		
	       		// in receiver thread, when you recv NACK  , then what?? to do
	       		
	       		return 2;
	       	
	       	}
	       	
	       	else
	       	{
		       	System.out.println("multicast duplicate, disgard it ");
	       		// S<=R, has already received this msg, just disgard it
	       		// 
	       		return 3;
       		}
        }

		private void checkHoldBackQueue(String groupID, String senders) 
		{
			//AtomicInteger lastSeqNum =  recvSeqTracker.get(groupID).get(sender);
			Iterator<MultiCastMessage> it = recvHoldBackQueue.iterator();
			
			while(it.hasNext())
			{
				MultiCastMessage tempTS = it.next();
	   	    	int senderSeq = tempTS.getMulticastSeq();
				for (String sender : conf.groupMap.get(groupID))
	   	    	if(tempTS.getSource().equals(sender) && tempTS.getGroupID().equals(groupID))
					if(checkOne(sender , tempTS , senderSeq) == 1 && checkACKs(tempTS) == true)
					{
						System.out.println("Holdback "+tempTS);
						recvQueue.offer(new MultiCastMessage(tempTS));
						recvMessageCache.get(groupID).get(sender).put(senderSeq % CacheSize, new MultiCastMessage(tempTS));
						recvSeqTracker.get(groupID).put(sender,new AtomicInteger(senderSeq));
						it.remove();
						it = recvHoldBackQueue.iterator();
						continue;
					}
			}
		}

    }
//    
//    public static void main(String[] args) throws Exception {
//        MessagePasser mp = new MessagePasser("/Users/maoqian/Dropbox/CMU/18842/p0/test_conf.txt", "me");
//        try {
//            mp.send(new Message("me", "1", "hello from myself: 1"));
//            mp.send(new Message("me", "2", "hello from myself: 2"));
//            mp.send(new Message("me", "3", "hello from myself: 3"));
//            mp.send(new Message("me", "4", "hello from myself: 4"));
//
//            Message receiveMsg = null;
//            while (true) {
//                receiveMsg = mp.receive();
//                if (receiveMsg != null) {
//                    System.out.println((String)receiveMsg.toString());
//                }
//            }
//            
//        } catch (UnknownHostException e) {
//            e.printStackTrace();
//        }
//        
//    }
}	
