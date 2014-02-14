package lab;
import java.io.*;
import java.net.*;
import java.util.logging.*;

/*
 * NetComm - UDP blocking version
 * Author: Qian Mao (qianm)
 *         Utsav Drolia (udrolia)
 * Date: 1/18/2014
 */

public class NetComm {

    // variables
    private DatagramSocket dSocket;
    private Logger log;

    // constructor
    public NetComm(int socket) {
        try {
            dSocket = new DatagramSocket(socket);
        } catch (SocketException e) {
            e.printStackTrace();
        }

        log = Logger.getLogger("NetComm");

    }

    // Serialize Message and send to Destination over UDP
    void NetSend(Message M, InetAddress destAddress, int port) {
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
            dSocket.send(Packet2Send);
        }

        catch (IOException e) {
            e.printStackTrace();
        }

    }

    // Receive from Destination over UDP and unserialize Message
    Message NetReceive() {
        byte[] buffer = new byte[1000];
        Message M = null;
        ByteArrayInputStream bis = null;
        ObjectInputStream objectReceived = null;

        // Create packet
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

        try {
            dSocket.receive(packet);

            bis = new ByteArrayInputStream(packet.getData());
            objectReceived = new ObjectInputStream(bis);
            M = (Message) objectReceived.readObject();

        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return M;
    }

    void NetClose() {
        if (dSocket != null)
            dSocket.close();
    }

}
