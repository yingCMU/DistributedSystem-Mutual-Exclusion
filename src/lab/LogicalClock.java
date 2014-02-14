package lab;

public class LogicalClock extends ClockService
{
	//Counter/Clock
	private Integer counter = 0;
	final static Integer INCREMENT = 1;

	public LogicalClock(String source) 
	{
		super(Clock.LOGICAL,source);
		counter = 0;
	}

	@Override
	public void updateClockOnReceive(TimeStamp ts) 
	{
		LogicalTimeStamp lts = (LogicalTimeStamp) ts;
		if(lts.getTime() > counter)
			counter = lts.getTime() + INCREMENT;
		else 
			counter = counter + INCREMENT;
	}

	@Override
	public void updateClockOnSend() 
	{		
		counter = counter + INCREMENT;
	}

	@Override
	public TimeStamp getCurrentTimeStamp() 
	{
		LogicalTimeStamp lts = new LogicalTimeStamp(source, counter);
		return lts;
	}

	@Override
	public String toString() 
	{	
		return new String (source + ":" + counter.toString());
	}

}
