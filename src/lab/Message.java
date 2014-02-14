package lab;
import java.io.Serializable;

/*
 * Message
 * Author: Qian Mao (qianm)
 *         Utsav Drolia (udrolia)
 * Date: 1/18/2014
 */

@SuppressWarnings("serial")
public class Message implements Serializable {
    protected String source = null;
    protected String dest = null;
    protected String kind = null;
    protected Object data = null;
    protected int seqNum = -1;
    protected Boolean dupe = false;
    protected int size = 0;
    protected LockType lockType = LockType.NA;
    
    public Message(String data){
    	this.data = data;
    	
    }
    public Message(String dest, String kind, Object data) {
        this.dest = dest;
        this.kind = kind;
        this.data = data;
    }
    
    // copy constructor
    public Message(Message origMsg) {
        this.source = origMsg.getSource();
        this.dest = origMsg.getDest();
        this.kind = origMsg.getKind();
        this.data = origMsg.getData();
        this.seqNum = origMsg.getSeqNum();
        this.dupe = origMsg.getDupe();
        this.size = origMsg.getSize();
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getDest() {
        return dest;
    }

    public void setDest(String dest) {
        this.dest = dest;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public int getSeqNum() {
        return seqNum;
    }

    public void setSeqNum(int seqNum) {
        this.seqNum = seqNum;
    }

    public Boolean getDupe() {
        return dupe;
    }

    public void setDupe(Boolean dupe) {
        this.dupe = dupe;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    // These settors are used by MessagePasser.send, not your app  
    public void set_source(String source) {
        this.source = source;
    }
    
    public void set_seqNum(int sequenceNumber) {
        this.seqNum = sequenceNumber;
    }
    
    public void set_duplicate(Boolean dupe) {
        this.dupe = dupe;
    }

    // other accessors, toString, etc as needed    
    public String toString() {
    	System.out.println("Locktype :"+lockType);
        return String.format("msg--- \nsource: %s, dest: %s, kind: %s, seqNum: %d, dupe: %s, size: %d, data: %s", source, dest, kind, seqNum, dupe.toString(), size, data.toString());
    }

	public LockType getLockType() {
		return lockType;
	}

	public void setLockType(LockType lockType) {
		this.lockType = lockType;
	}

}