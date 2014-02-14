package lab;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;

/*
 * TCPNetCommThreaded - TCP non-blocking version
 * Author: Ying Li (yingl2)
 *         Utsav Drolia (udrolia)
 * Date: 2/8/2014
 */

public class TCPNetCommThreaded {
    // variables
    // ServerSocket socketAccept;
    int portAccept;
    ConcurrentLinkedQueue<OutgoingMessage> sendQueue;
    ConcurrentLinkedQueue<TimeStampedMessage> receiveQueue;

    AcceptThread acceptThread;
    NetSendThread sendThread;

    Thread Accept;
    Thread Send;

    Logger LOG;

    // constructor
    TCPNetCommThreaded(int port) {
        // port
        portAccept = port;

        // Sender Thread
        sendQueue = new ConcurrentLinkedQueue<OutgoingMessage>();
        sendThread = new NetSendThread(sendQueue);
        Send = new Thread(sendThread);

        // Accept Thread
        receiveQueue = new ConcurrentLinkedQueue<TimeStampedMessage>();
        acceptThread = new AcceptThread(port, receiveQueue);
        Accept = new Thread(acceptThread);

        LOG = Logger.getLogger("NetComm");
        Send.start();
        Accept.start();
    }

    public void NetSend(TimeStampedMessage M, InetAddress destAddress, int port) {
        OutgoingMessage m = new OutgoingMessage(M, destAddress, port);
        sendQueue.offer(m);

    }

    TimeStampedMessage NetReceive() {
    	TimeStampedMessage M = receiveQueue.poll();
        return M;
    }

    private class OutgoingMessage {
        public TimeStampedMessage M;
        public Node node;

        public class Node {
            public InetAddress destAddress;
            public int port;

            Node(InetAddress destAddress, int port) {
                super();
                this.destAddress = destAddress;
                this.port = port;
            }
        }

        public OutgoingMessage(TimeStampedMessage m, InetAddress destAddress, int port) {
            super();
            M = m;
            node = new Node(destAddress, port);
        }
    }

    private class AcceptThread implements Runnable {
        // variables
        ServerSocket acceptSocket;
        ConcurrentLinkedQueue<TimeStampedMessage> receiveQueue;

        AcceptThread(int port, ConcurrentLinkedQueue<TimeStampedMessage> q) {
            receiveQueue = q;
            try {
                acceptSocket = new ServerSocket(port);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        void AcceptSocketClose() throws IOException {
            if (acceptSocket != null) {
                acceptSocket.close();
            }
        }

        public void run() {
            try {
                while (true) {
                    new NetReceiveThread(receiveQueue, acceptSocket.accept())
                            .start();

                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    private class NetSendThread implements Runnable {
        // variables
        ConcurrentLinkedQueue<OutgoingMessage> sendQueue;
        private HashMap<OutgoingMessage.Node, Socket> registeredNodes;

        NetSendThread(ConcurrentLinkedQueue<OutgoingMessage> q) {
            sendQueue = q;
            registeredNodes = new HashMap<OutgoingMessage.Node, Socket>();
        }

        // methods
        // Serialize Message and send to Destination over UDP
        public void run() {
            while (true) {
                OutgoingMessage OM = sendQueue.poll();

                try {
                    if (OM != null) {
                        if (registeredNodes.get(OM.node) == null) {
                            Socket tempSocket = new Socket(OM.node.destAddress,
                                    OM.node.port);
                            registeredNodes.put(OM.node, tempSocket);
                        }

                        LOG.info(OM.M.toString());
                        TimeStampedMessage M = OM.M;
                        Socket tempSocket = registeredNodes.get(OM.node);

                        // byte[] buffer = null;
                        // ByteArrayOutputStream bos = new
                        // ByteArrayOutputStream();
                        ObjectOutputStream objectSent;

                        objectSent = new ObjectOutputStream(
                                tempSocket.getOutputStream());

                        // Serialize Message M and write to output stream
                        objectSent.writeObject(M);
                        Thread.sleep(10);
                        // buffer = bos.toByteArray();
                    }
                } catch (IOException e) {
                    System.out.println("Remote Peer closed");
                    registeredNodes.put(OM.node, null);
                } catch (InterruptedException e) {
					e.printStackTrace();
				}

                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    // Receive from Destination over UDP and unserialize Message

    public class NetReceiveThread extends Thread {
        // variables
        ConcurrentLinkedQueue<TimeStampedMessage> receiveQueue;
        Socket receiveSocket;

        // constructor
        NetReceiveThread(ConcurrentLinkedQueue<TimeStampedMessage> q, Socket socket) {
            receiveQueue = q;
            receiveSocket = socket;
        }

        public void run() {
            ObjectInputStream objectReceived = null;
            try {
                objectReceived = new ObjectInputStream(
                        receiveSocket.getInputStream());
                while (!receiveSocket.isClosed()) {
                    TimeStampedMessage M = null;

                    M = (TimeStampedMessage) objectReceived.readObject();

                    if (M != null){
                    	//System.out.println("## in TCPNetCOmmThread: "+M.toString());
                        receiveQueue.offer(M);
                    }

                    Thread.sleep(100);
                }
            } catch (ClassNotFoundException | InterruptedException e) {
                e.printStackTrace();
            } catch (IOException e) {
                System.out.println("Remote Peer closed");
            }
        }
    }

    void NetClose() throws IOException {
        acceptThread.AcceptSocketClose();
    }

}
