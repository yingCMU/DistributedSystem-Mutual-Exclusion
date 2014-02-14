package lab;
import java.util.LinkedList;
import java.util.ListIterator;

public class Loggerclass {    
	private LinkedList<TimeStampedMessage>  list = new LinkedList<TimeStampedMessage> ();
	private LinkedList<TimeStampedMessage>  before = new LinkedList<TimeStampedMessage> ();
	private LinkedList<TimeStampedMessage>  after = new LinkedList<TimeStampedMessage> ();
	private LinkedList<TimeStampedMessage>  concurrent = new LinkedList<TimeStampedMessage> ();
	private LinkedList<TimeStampedMessage>  equal = new LinkedList<TimeStampedMessage> ();

    
	public void displaylog()
	{
		printsortedlog();
	}

	public void printunsortedlog(){
		ListIterator<TimeStampedMessage> itr = list.listIterator();
		while(itr.hasNext()){
			TimeStampedMessage currentmessage = (TimeStampedMessage) itr.next();
			String data = (String) currentmessage.getData();
			System.out.printf("The data is %s\n", data);
		}
	}

	public void printlog(){
		ListIterator<TimeStampedMessage> itr = before.listIterator();
		if(itr.hasNext())
			System.out.printf("Messages->Current message\n");
		while(itr.hasNext()){
			TimeStampedMessage currentmessage = (TimeStampedMessage) itr.next();
			String data = (String) currentmessage.getData();
			System.out.printf("DATA: %s\n", data);
		}

		itr = concurrent.listIterator();
		if(itr.hasNext())
			System.out.printf("Messages (||) current message\n");
		while(itr.hasNext()){
			TimeStampedMessage currentmessage = (TimeStampedMessage) itr.next();
			String data = (String) currentmessage.getData();
			System.out.printf("DATA: %s\n", data);
		}

		itr = after.listIterator();
		if(itr.hasNext())
			System.out.printf("Current message->Messages\n");
		while(itr.hasNext()){
			TimeStampedMessage currentmessage = (TimeStampedMessage) itr.next();
			String data = (String) currentmessage.getData();
			System.out.printf("DATA: %s\n", data);
		}
		
		itr = equal.listIterator();
		if(itr.hasNext())
			System.out.printf("Current message=Messages\n");
		while(itr.hasNext()){
			TimeStampedMessage currentmessage = (TimeStampedMessage) itr.next();
			String data = (String) currentmessage.getData();
			System.out.printf("DATA: %s\n", data);
		}
		
		concurrent.clear();
		before.clear();
		after.clear();
		equal.clear();
	}

	public void printsortedlog(){
		for (int i = 0; i < list.size(); i++) {
			TimeStamp current = list.get(i).getTimeStamp();
			String data = (String) list.get(i).getData();
			System.out.printf("Current message is: %s\n", data);
			for(int j=0; j< list.size(); j++)
			{
				//So that you do not add yourself into the before after array
				if(j==i)
					continue;
				TimeStamp innercurrent = list.get(j).getTimeStamp(); 
				int compareoutput = current.compareTo(innercurrent);
				
				//This is to handle if logical clocks ar compared,
				//You just print unsorted array and return
				if(compareoutput == TimeCompare.ERROR){
					this.printunsortedlog();
					return;
				}
				else if(compareoutput == TimeCompare.EQUALS)
					equal.add(list.get(j));
				//Current is less than innercurrent
				else if(compareoutput == TimeCompare.BEFORE)
					after.add(list.get(j));
				//Current and inner current are concurrent
				else if(compareoutput == TimeCompare.CONCURRENT)
					concurrent.add(list.get(j));
				//
				else if(compareoutput == TimeCompare.AFTER)
					before.add(list.get(j));
			}
			this.printlog();
		}
	}

	public void addlog(TimeStampedMessage message){
		list.add(message);
	}
}
