package lab;

import java.io.Serializable;

@SuppressWarnings("serial")
public class NACKRequest implements Serializable
{
	//fields
	
	//Source of requested message
	private String source;
	
	//Seq Num of first requested message
	private int firstSeqNum;
	
	//Seq Num of last requested message
	private int lastSeqNum;

	public NACKRequest(String sender, int firstSeq, int lastSeq) 
	{	
		source = sender;
		firstSeqNum = firstSeq;
		lastSeqNum = lastSeq;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public int getFirstSeqNum() {
		return firstSeqNum;
	}

	public void setFirstSeqNum(int seqNum) {
		this.firstSeqNum = seqNum;
	}

	public int getLastSeqNum() {
		return lastSeqNum;
	}

	public void setLastSeqNum(int lastSeqNum) {
		this.lastSeqNum = lastSeqNum;
	}
	
	public String toString()
	{
		return new String("Source:" + source + " StartSeq:" + firstSeqNum + " EndSeq:" + lastSeqNum);
	}

}
