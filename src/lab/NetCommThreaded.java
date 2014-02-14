package lab;
import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.*;



/*
 * NetCommThreaded - UDP non-blocking version
 * Author: Qian Mao (qianm)
 *         Utsav Drolia (udrolia)
 * Date: 1/18/2014
 */

public class NetCommThreaded {

    // variables
    DatagramSocket DSocketReceive;
    DatagramSocket DSocketSend;
    ConcurrentLinkedQueue<OutgoingMessage> sendQueue;
    ConcurrentLinkedQueue<Message> receiveQueue;
    

    NetReceiveThread receiveThread;
    NetSendThread sendThread;

    Thread Receive;
    Thread Send;

    Logger LOG;
	

    // constructor
    NetCommThreaded(int socket) {
        // Sender Thread
        sendQueue = new ConcurrentLinkedQueue<OutgoingMessage>();
        sendThread = new NetSendThread(sendQueue);
        Send = new Thread(sendThread);

        // Receiver Thread
        receiveQueue = new ConcurrentLinkedQueue<Message>();
        receiveThread = new NetReceiveThread(receiveQueue);
        
        Receive = new Thread(receiveThread);

        try {
            DSocketReceive = new DatagramSocket(socket);
            DSocketSend = new DatagramSocket();
        } catch (SocketException e) {
            e.printStackTrace();
        }

        LOG = Logger.getLogger("NetComm");
        Send.start();
        Receive.start();
    }

    public void NetSend(Message M, InetAddress destAddress, int port) {
        OutgoingMessage m = new OutgoingMessage();
        m.M = M;
        m.destAddress = destAddress;
        m.port = port;
        sendQueue.offer(m);

    }

    Message NetReceive() {
        Message M = receiveQueue.poll();
        return M;
    }

    public class OutgoingMessage {
        Message M;
        InetAddress destAddress;
        int port;
    }

    public class NetSendThread implements Runnable {
        // variables
        ConcurrentLinkedQueue<OutgoingMessage> sendQueue;

        NetSendThread(ConcurrentLinkedQueue<OutgoingMessage> q) {
            sendQueue = q;
        }

        // methods
        // Serialize Message and send to Destination over UDP
        public void run() {
            while (!DSocketSend.isClosed()) {
                OutgoingMessage OM = sendQueue.poll();

                if (OM != null) {
                    LOG.info("send");
                    Message M = OM.M;
                    InetAddress destAddress = OM.destAddress;
                    int port = OM.port;
                    byte[] buffer = null;
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    ObjectOutputStream objectSent;

                    try {
                        objectSent = new ObjectOutputStream(bos);

                        // Serialize Message M to bytes
                        objectSent.writeObject(M);
                        buffer = bos.toByteArray();

                        // Construct Packet
                        DatagramPacket Packet2Send = new DatagramPacket(buffer,
                                buffer.length, destAddress, port);

                        // Send packet
                        DSocketSend.send(Packet2Send);
                    }

                    catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

        }
    }

    // Receive from Destination over UDP and unserialize Message

    public class NetReceiveThread implements Runnable {
        // variables
        ConcurrentLinkedQueue<Message> receiveQueue;
        HashMap<String, AtomicInteger> recvSeqTracker=new HashMap<String, AtomicInteger>();
        // constructor
        NetReceiveThread(ConcurrentLinkedQueue<Message> q) {
            receiveQueue = q;
        }
        

        public void run() {
            while (!DSocketReceive.isClosed()) {
                byte[] buffer = new byte[1000];
                Message M = null;
                ByteArrayInputStream bis = null;
                ObjectInputStream objectReceived = null;

                // Create packet
                DatagramPacket packet = new DatagramPacket(buffer,
                        buffer.length);

                try {
                    LOG.log(Level.INFO, "Waiting for Packet");
                    DSocketReceive.receive(packet);
                    LOG.log(Level.INFO, "Got Packet");

                    bis = new ByteArrayInputStream(packet.getData());
                    objectReceived = new ObjectInputStream(bis);
                    M = (Message) objectReceived.readObject();

                } catch (IOException e) {
                    e.printStackTrace();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
                
                	
                receiveQueue.offer(M);
            }
        }
    }
   
    void NetClose() {
        if (DSocketReceive != null)
            DSocketReceive.close();
        if (DSocketSend != null)
            DSocketSend.close();
    }

}
