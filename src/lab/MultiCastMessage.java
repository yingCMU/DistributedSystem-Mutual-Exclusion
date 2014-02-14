package lab;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings("serial")
public class MultiCastMessage extends TimeStampedMessage
{
	
	private ControlType controlType = ControlType.NORMAL;
	private String groupID = null;
	private int multicastSeq = -1;
	private HashMap<String,AtomicInteger> ACKs = new HashMap<String,AtomicInteger>();

	public MultiCastMessage(TimeStampedMessage origMsg) 
	{
		super(origMsg);
		
			MultiCastMessage origmsg = (MultiCastMessage)origMsg;
			setControlType(origmsg.getControlType());
			setGroupID(origmsg.getGroupID());
			setMessageType(origmsg.getMessageType());
			setACKs(origmsg.getACKs());
			setMulticastSeq(origmsg.getMulticastSeq());
			//System.out.println("## in Constructor MulticastMessage "+origMsg.lockType);
			setLockType(origmsg.lockType);

		
	}
	
	public MultiCastMessage(String dest, String kind, Object data, TimeStamp timeStamp, String groupID, int multicastSeq, HashMap<String,AtomicInteger> ACKs, ControlType ctype)
	{
		super( dest,  kind,  data,  timeStamp);
		
		this.groupID = groupID;
		this.multicastSeq = multicastSeq; 
		this.ACKs = ACKs;
		this.controlType = ctype;
	}
	
	public MultiCastMessage(MultiCastMessage origMsg) 
	{
		super(origMsg);
		setControlType(origMsg.getControlType());
		setGroupID(origMsg.getGroupID());
		setMessageType(origMsg.getMessageType());
		setACKs(origMsg.getACKs());
		setMulticastSeq(origMsg.getMulticastSeq());
	}
	
	

	public MultiCastMessage(String data) {
		super(data);
	}

	public MultiCastMessage(String string, String string2, Object nackRequest, TimeStamp object) {
		super(string,string2,nackRequest,object);
	}

	public MessageType getMessageType() {
		return messageType;
	}

	public void setMessageType(MessageType messageType) {
		this.messageType = messageType;
	}

	public ControlType getControlType() {
		return controlType;
	}

	public void setControlType(ControlType controlType) {
		this.controlType = controlType;
	}

	public String getGroupID() {
		return groupID;
	}

	public void setGroupID(String groupID) {
		this.groupID = groupID;
	}
	
	public HashMap<String,AtomicInteger> getACKs() {
		return ACKs;
	}

	public void setACKs(HashMap<String,AtomicInteger> aCKs) {
		ACKs = new HashMap<String,AtomicInteger>(aCKs);
	}

	public int getMulticastSeq() {
		return multicastSeq;
	}

	public void setMulticastSeq(int multicastSeq) {
		this.multicastSeq = multicastSeq;
	}

	public String toString()
	{
		return new String(super.toString() + ", GroupID:" + this.groupID + ", MultiSeq:" + this.multicastSeq + ", ACKS:" + this.ACKs.toString() + ", Control:"+this.controlType);
		
	}

	
	

}
