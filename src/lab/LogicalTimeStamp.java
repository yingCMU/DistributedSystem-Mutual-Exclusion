package lab;

@SuppressWarnings("serial")
public class LogicalTimeStamp  extends TimeStamp
{

	private Integer time = null;

	public LogicalTimeStamp(String source) 
	{
		super(source, Clock.LOGICAL);
	}
	
	//Constructor2
	public LogicalTimeStamp(String source, Integer time)
	{
		super(source,Clock.LOGICAL);
		this.time = time;
	}
	
	//Get time value
	public Integer getTime() 
	{
		return time;
	}
	
	//Set time value
	public void setTime(Integer time) 
	{
		this.time = time;
	}
	
	
	@Override
	public String toString()
	{
		return new String(source + ":" + time);
		
	}
	
	@Override
	public int compareTo(TimeStamp o) {
		System.out.println("Cannot compare logical clocks");
		return TimeCompare.ERROR;
	}
}


/*
public boolean greaterThanEqual(TimeStamp ts)
{
//	LogicalTimeStamp lts = (LogicalTimeStamp) ts;
//	if(this.getTime() > lts.getTime())
//		return true;
//	else
//		return false;
	System.out.println("Illegal Operation");
	return false;
	
}

public boolean lesserThanEqual(TimeStamp ts)
{
//	LogicalTimeStamp lts = (LogicalTimeStamp) ts;
//	if(this.getTime() < lts.getTime())
//		return true;
//	else
//		return false;
	System.out.println("Illegal Operation");
	return false;
}

public boolean equalTo(TimeStamp ts)
{
//	LogicalTimeStamp lts = (LogicalTimeStamp) ts;
//	if(this.getTime() == lts.getTime())
//		return true;
//	else
//		return false;
	System.out.println("Illegal Operation");
	return false;
}
*/
