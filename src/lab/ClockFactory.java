package lab;

import java.util.List;

public class ClockFactory 
{
	public static ClockService getClockService(Clock type, String host, List<String> list)
	{
		ClockService clock = null;
		switch (type) 
		{
		case LOGICAL:
			clock = new LogicalClock(host);
			break;
		case VECTOR:
			clock = new VectorClock(host, list);
			break;
			
		default:
			throw new IllegalArgumentException();
		
		}
		return clock;
	}

}
