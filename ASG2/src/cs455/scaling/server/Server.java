package cs455.scaling.server;

import cs455.scaling.threadpool.AcceptTask;
import cs455.scaling.threadpool.ReadTask;
import cs455.scaling.threadpool.ThreadPool;
import cs455.scaling.util.Util;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by crt on 2/18/14.
 */
public class Server implements Runnable {

    private InetAddress hostAddress;
    private int port;

    private ServerSocketChannel serverChannel;  // channel to accept connections
    private Selector selector;                  // selector to monitor
    private ThreadPool threadPool = null;
    private List<ClientInfo> clientList;

    public Server(InetAddress host, int port) throws IOException{
        this.hostAddress = host;
        this.port = port;
        this.selector = this.initializeSelector();
        this.threadPool = new ThreadPool(3).initialize();
        this.clientList = new ArrayList<ClientInfo>();
    }

    // opens our selector on the specified host/port
    private Selector initializeSelector() throws IOException {
        Selector socketSelector = SelectorProvider.provider().openSelector();

        // create non-blocking channel and bind it to our host and port
        this.serverChannel = ServerSocketChannel.open();
        serverChannel.configureBlocking(false);
        serverChannel.socket().bind(new InetSocketAddress(getHost(), getPort()));

        // invite connections
        SelectionKey selectionKey = serverChannel.register(socketSelector, SelectionKey.OP_ACCEPT);
        System.out.println("Server information:" + selectionKey.channel().toString());
        return socketSelector;
    }

    @Override
    public void run() { // accept connections and send incoming reads to our threadpool to handle
        while(true) {
            try {
                this.selector.select(); // get an event from a registered channel
                for(SelectionKey key : this.selector.selectedKeys()) {
                    if(!key.isValid()) continue;

                    //System.out.println("handling key from:"+key.channel().toString());

                    this.selector.selectedKeys().remove(key);

                    // handle the event for this key
                    /*
                    if(key.isAcceptable())
                        ((new AcceptTask(key, this.selector))).execute();
                    else if(key.isReadable()) {
                        (new ReadTask(key)).execute();
                    }
                    */
                    if(key.isAcceptable()) {
                        ClientInfo clientInfo = new ClientInfo((ServerSocketChannel) key.channel());

                        System.out.println("new accept key");
                        System.out.println(clientList.contains(clientInfo));

                        // dont let the key for a client get in twice or hell breaks loose
                        if(clientList.contains(clientInfo)) {
                            //key.cancel();
                            //System.out.println("canceled key");
                            continue;
                        }
                        clientList.add(clientInfo);

                        threadPool.addTaskToExecute((new AcceptTask(key, this.selector)));

                    } else if(key.isReadable()) {
                        threadPool.addTaskToExecute(new ReadTask(key));
                    }
                }
            }
            catch (IOException e)           { e.printStackTrace(); }
            catch (InterruptedException e)  { e.printStackTrace(); }
        }
    }

    private String getHost()    { return this.hostAddress.getHostName(); }
    private int getPort()       { return this.port; }

    public static void main(String[] args) {
        try {

            InetAddress host = InetAddress.getLocalHost();
            int port = 8080;
            if(args.length > 0) {
                port = Integer.parseInt(args[0]);
            }

            new Thread(new Server(host, port)).start();

        } catch (IOException e) { e.printStackTrace(); }
    }
}
