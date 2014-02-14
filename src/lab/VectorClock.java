package lab;

import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import lab.Clock;
import lab.ClockService;
import lab.TimeStamp;

public class VectorClock extends ClockService{
	private HashMap<String, Integer>  vectorclockmap= null;
	//ArrayList<Integer> time = new ArrayList<Integer>();
	//int myindex;

	public VectorClock(String localname, List<String> list) 
	{
		super(Clock.VECTOR, localname);
		vectorclockmap = new HashMap<String, Integer>();
		System.out.println("init.... "+vectorclockmap);
		for (String s : list)
		{
			vectorclockmap.put(s, 0);
		}
		//myindex = index;
	}

	@Override
	public TimeStamp getCurrentTimeStamp() {
		VectorTimeStamp vts = new VectorTimeStamp(source,vectorclockmap);
		//vts.setTime(vectorclockmap);
		return vts;
	}

	@Override
	public void updateClockOnReceive(TimeStamp ts) {
		VectorTimeStamp lts = (VectorTimeStamp) ts;
		HashMap<String, Integer> mp = lts.getTime();

		//If the two vector lengths are not equal return
	    if(mp.size() != vectorclockmap.size()){
			System.out.println("The number of entries in both the timestamps are unequal");
			return;
		}
	    
	    java.util.Iterator<Entry<String, Integer>> it = mp.entrySet().iterator();
	    while (it.hasNext()) {
	        Entry<String, Integer> pairs = it.next();
	        //If the entry your reading is yourself, ignore and increment your value by 1
	        if(pairs.getKey().equals(source))
	        	vectorclockmap.put(source, vectorclockmap.get(source)+1);
	        else
	        {
	        	//Get the value for the same src of both and compare, assign
	        	//the higher value for that src
	        	if(vectorclockmap.get(pairs.getKey()) < pairs.getValue())
	        		vectorclockmap.put(pairs.getKey(), pairs.getValue());
	        }
	    }
	}

	@Override
	public void updateClockOnSend() {
		//System.out.println("updateonsend "+vectorclockmap);
		vectorclockmap.put(source, vectorclockmap.get(source)+1);
		
	}

	@Override
	public String toString() 
	{
		return new String(source + "::" + vectorclockmap.toString());
	}
	
}
