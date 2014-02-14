package lab;
import java.util.HashMap;
import java.util.Map.Entry;

@SuppressWarnings("serial")
public class VectorTimeStamp extends TimeStamp{
	/**
	 * 
	 */
	private HashMap<String, Integer>  time = null;


	public VectorTimeStamp(String source, HashMap<String, Integer> time) 
	{
		super(source, Clock.VECTOR);
		this.time = new  HashMap<String, Integer>(time);
	}

	@Override
	public String toString() 
	{
		return new String(source + "::" + time.toString());
	}

	public HashMap<String, Integer> getTime() 
	{
		return time;
	}

	public void setTime(HashMap<String, Integer> time) 
	{
		this.time = time;
	}
	
	@Override
	public int compareTo(TimeStamp ts) {

		//final int BEFORE = -1;
		//final int EQUAL = 0;
		//final int AFTER = 1;
		//final int SAME = 2;
		
		VectorTimeStamp lts = (VectorTimeStamp) ts;
		HashMap<String, Integer> mp = lts.getTime();
		if(mp.size() != this.time.size()){
			System.out.println("The number of entries in both the timestamps are unequal");
			return TimeCompare.ERROR;
		}
		
		//Compare if they are equal
		int count = 0;
		java.util.Iterator<Entry<String, Integer>> it = mp.entrySet().iterator();
		while (it.hasNext()) {
			Entry<String, Integer> pairs = it.next();
			if(this.time.get(pairs.getKey()).equals(pairs.getValue()))
				count++;
		}
		if(count == mp.size()){
			return TimeCompare.EQUALS;
		}

		//Compare for happened before relationship
		count = 0;
		it = mp.entrySet().iterator();
		while (it.hasNext()) {
			Entry<String, Integer> pairs = it.next();
			if(this.time.get(pairs.getKey()) <= pairs.getValue())
				count++;
		}
		if(count == mp.size()){
			return TimeCompare.BEFORE;
		}

		//Compare for happened after relationship
		count = 0;
		it = mp.entrySet().iterator();
		while (it.hasNext()) {
			Entry<String, Integer> pairs = it.next();
			if(time.get(pairs.getKey()) >= pairs.getValue())
				count++;
		}
		if(count == mp.size()){
			return TimeCompare.AFTER;
		}
		
		//If none of them hit return equal meaning they are concurrent
		return TimeCompare.CONCURRENT;
	}	
}
/*
	public boolean greaterThanEqual(TimeStamp ts) {
		VectorTimeStamp lts = (VectorTimeStamp) ts;
		HashMap<String, Integer> mp = lts.getTime();
		if(mp.size() != time.size()){
			System.out.println("The number of entries in both the timestamps are unequal");
			return false;
		}
	    java.util.Iterator<Entry<String, Integer>> it = mp.entrySet().iterator();
	    while (it.hasNext()) {
	        Entry<String, Integer> pairs = it.next();

	        if(time.get(pairs.getKey()) >= pairs.getValue())
	        	continue;
	        else
	        	return false;
	    }
		return true;
	}

	@Override
	public boolean lesserThanEqual(TimeStamp ts) {
		VectorTimeStamp lts = (VectorTimeStamp) ts;
		HashMap<String, Integer> mp = lts.getTime();
		if(mp.size() != time.size()){
			System.out.println("The number of entries in both the timestamps are unequal");
			return false;
		}
	    java.util.Iterator<Entry<String, Integer>> it = mp.entrySet().iterator();
	    while (it.hasNext()) {
	        Entry<String, Integer> pairs = it.next();

	        if(time.get(pairs.getKey()) <= pairs.getValue())
	        	continue;
	        else
	        	return false;
	    }
		return true;
	}

	@Override
	public boolean equalTo(TimeStamp ts) {
		VectorTimeStamp lts = (VectorTimeStamp) ts;
		HashMap<String, Integer> mp = lts.getTime();
		if(mp.size() != time.size()){
			System.out.println("The number of entries in both the timestamps are unequal");
			return false;
		}
	    java.util.Iterator<Entry<String, Integer>> it = mp.entrySet().iterator();
	    while (it.hasNext()) {
	        Entry<String, Integer> pairs = it.next();

	        if(time.get(pairs.getKey()) == pairs.getValue())
	        	continue;
	        else
	        	return false;
	    }
		return true;
	}
 */

/*
	ArrayList<Integer> time = new ArrayList<Integer>();

	public VectorTimeStamp(String source) {
		super(source);
		// TODO Auto-generated constructor stub
	}

	//Setters and Getters
	public ArrayList<Integer> getTime() {
		return time;
	}

	public void setTime(ArrayList<Integer> time) {
		this.time = time;
	}

	@Override
	public boolean greaterThan(TimeStamp ts) {
		VectorTimeStamp lts = (VectorTimeStamp) ts;
		ArrayList<Integer> Timestampcurrent = this.getTime();
		ArrayList<Integer> Timestamplts = lts.getTime();
		if (Timestampcurrent.size() != Timestamplts.size()){
			System.out.println("The two timestamps have unequal fields");
			return false;
		}
		else{
			for(int i=0;i<Timestampcurrent.size();i++){
				if(Timestampcurrent.get(i) >= Timestamplts.get(i))
					continue;
				else
					return false;
			}
		}
		return true;
	}

	@Override
	public boolean lesserThan(TimeStamp ts) {
		VectorTimeStamp lts = (VectorTimeStamp) ts;
		ArrayList<Integer> Timestampcurrent = this.getTime();
		ArrayList<Integer> Timestamplts = lts.getTime();
		if (Timestampcurrent.size() != Timestamplts.size()){
			System.out.println("The two timestamps have unequal fields");
			return false;
		}
		else{
			for(int i=0;i<Timestampcurrent.size();i++){
				if(Timestampcurrent.get(i) <= Timestamplts.get(i))
					continue;
				else
					return false;
			}
		}
		return true;
	}

	@Override
	public boolean equalTo(TimeStamp ts) {
		VectorTimeStamp lts = (VectorTimeStamp) ts;
		ArrayList<Integer> Timestampcurrent = this.getTime();
		ArrayList<Integer> Timestamplts = lts.getTime();
		if (Timestampcurrent.size() != Timestamplts.size()){
			System.out.println("The two timestamps have unequal fields");
			return false;
		}
		else{
			for(int i=0;i<Timestampcurrent.size();i++){
				if(Timestampcurrent.get(i) == Timestamplts.get(i))
					continue;
				else
					return false;
			}
		}
		return true;
	}

 */
