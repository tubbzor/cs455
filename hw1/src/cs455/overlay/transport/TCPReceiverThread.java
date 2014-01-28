package cs455.overlay.transport;
import cs455.overlay.node.*;
import java.io.*;
import java.net.*;
public class TCPReceiverThread { //extends Thread {

    private Socket socket;
    private Node node;
    private DataInputStream din;

    public TCPReceiverThread (Node node, Socket socket) throws IOException {
        this.node = node;
        this.socket = socket;
        din = new DataInputStream(socket.getInputStream());
    }

    //public void run() {
    public void start() {
        int dataLen;
        //while(socket != null) {
            try {
                dataLen = din.readInt();
                byte[] data = new byte[dataLen];
                din.readFully(data, 0, dataLen);
                node.onEvent(new String(data));
            } catch(SocketException se) {
                System.out.println(se.getMessage());
                //break;
            } catch(IOException ioe) {
                System.out.println(ioe.getMessage());
                //break;
            }
        //}
    }
}
