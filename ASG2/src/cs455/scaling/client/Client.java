package cs455.scaling.client;

import cs455.scaling.util.Util;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;

/**
 * Created by ctebbe on 2/20/14.
 */
public class Client implements Runnable {

    private InetAddress hostAddress;
    private int port;

    private Selector selector;

    public Client(InetAddress host, int port) throws IOException {
        this.hostAddress = host;
        this.port = port;
        this.selector = SelectorProvider.provider().openSelector();
        initiateConnection();
    }

    public void send(byte[] dataToSend) throws IOException {

    }

    private SocketChannel initiateConnection() throws IOException {
        // open a non-blocking channel
        SocketChannel channel = SocketChannel.open();
        channel.configureBlocking(false);

        // connect to our host and port
        channel.connect(new InetSocketAddress(getHost(), getPort()));

        // register with our selector
        selector = Selector.open();
        channel.register(selector, SelectionKey.OP_CONNECT);

        return channel;
    }

    @Override
    public void run() {
        try {
            while(selector.select() > 0) {
                for(SelectionKey key : selector.selectedKeys()) {
                    selector.selectedKeys().remove(key);
                    SocketChannel myChan = (SocketChannel) key.channel();

                    if(key.isConnectable()) {
                        if(myChan.isConnectionPending()) {
                            myChan.finishConnect();
                            System.out.println("is conn?"+myChan.isConnected());
                        }
                    }

                    // generate data to send
                    ByteBuffer byteBuffer = ByteBuffer.allocate(Util.BUFFER_SIZE);
                    byteBuffer.put(Util.generateRandomByteArray());

                    System.out.println("Attempting to send:"+byteBuffer.hashCode());
                    myChan.write(byteBuffer);
                }
            }

        } catch (IOException e) { e.printStackTrace(); }

        /*
        while(true) {
            try {

                this.selector.select();
                //for(SelectionKey key : )

            } catch (IOException e) { e.printStackTrace(); }
        }
        */
    }

    public int getPort() { return this.port; }
    public String getHost() { return this.hostAddress.getHostName(); }

    public static void main(String[] args) {
        try {

            new Thread(new Client(InetAddress.getLocalHost(), 8080)).start();

        } catch (IOException e) { e.printStackTrace(); }
    }
}
