package lab;
import java.io.Serializable;


@SuppressWarnings("serial")
public abstract class TimeStamp implements Serializable, Comparable<TimeStamp>
{
	//variables
	private Clock clockType = null;
	protected String source = null;
		
	//Constructor1
	public TimeStamp(String source, Clock type)
	{
		this.source = source;
		this.clockType = type;
	}
	
	public Clock getClockType() {
		return clockType;
	}

	public void setClockType(Clock clockType) {
		this.clockType = clockType;
	}

	//Get source node for timestamp
	public String getSource() 
	{
		return source;
	}

	//Set source node for timestamp
	public void setSource(String source) 
	{
		this.source = source;
	}
	
	//Compare TimeStamps
	/*
	public abstract boolean greaterThanEqual(TimeStamp ts);
	
	public abstract boolean lesserThanEqual(TimeStamp ts);
	
	public abstract boolean equalTo(TimeStamp ts);
	*/
	@Override
	public abstract String toString();

}
