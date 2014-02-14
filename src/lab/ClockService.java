package lab;

public abstract class ClockService {
	//variables
	private Clock clockType = null;
	protected String source;
	
	//Constructor
	public ClockService(Clock type, String source)
	{
		this.clockType = type;
		this.source = source;
	}
	
	public Clock getClockType()
	{
		return this.clockType;
	}
	
	public String getLocalName()
	{
		return this.source;
	}
	

	public void setLocalName(String localname) 
	{
		this.source = localname;
	}
	
	public abstract void updateClockOnReceive(TimeStamp ts);
	public abstract void updateClockOnSend();
	public abstract TimeStamp getCurrentTimeStamp();
	public abstract String toString();

}
