package lab;

public class Request implements Comparable<Request> {
	int time;
	String name;
	public Request(int time, String name){
		this.time = time;
		this.name = name;
		
	}
	@Override
	public int compareTo(Request o) {
		int comparedSize = o.time;
		if (this.time > comparedSize) {
			return 1;
		} else if (this.time == comparedSize) {
			return 0;
		} else {
			return -1;
		}
	}
	public String toString(){
		return "name: "+name+"; time: "+time;
	}
}
