package lab;

@SuppressWarnings("serial")
public class TimeStampedMessage extends Message
{
	private TimeStamp timeStamp = null;
	protected MessageType messageType = null;
	private LogicalTimeStamp requestTS = null;
	
	//Constructor
	public TimeStampedMessage(String dest, String kind, Object data, TimeStamp timeStamp) 
	{
		super( dest,  kind,  data);
		this.setTimeStamp(timeStamp);
	}
	
	//Copy Constructor
	public TimeStampedMessage(TimeStampedMessage origMsg)
	{
		super(origMsg);
		this.timeStamp = origMsg.getTimeStamp();
		setLockType(origMsg.lockType);
		setRequestTS(origMsg.getRequestTS());
	}
	
	
	

	public TimeStampedMessage(String data) {
		super(data);
	}

	public TimeStamp getTimeStamp() 
	{
		return timeStamp;
	}

	public void setTimeStamp(TimeStamp timeStamp) 
	{
		this.timeStamp = timeStamp;
	}
	

	public String toString()
	{
		return new String(super.toString() + ", TimeStamp:" + this.timeStamp.toString());
		
	}

	public LogicalTimeStamp getRequestTS() {
		return requestTS;
	}

	public void setRequestTS(LogicalTimeStamp requestTS) {
		this.requestTS = requestTS;
	}

}
