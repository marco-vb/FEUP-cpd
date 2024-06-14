package pt.up.fe.cpd2324.server;

import pt.up.fe.cpd2324.client.Player;
import pt.up.fe.cpd2324.common.Connection;
import pt.up.fe.cpd2324.common.TreeSet;
import pt.up.fe.cpd2324.common.Utils;
import pt.up.fe.cpd2324.queue.NormalQueue;
import pt.up.fe.cpd2324.queue.RankedQueue;

import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;

public class Server {
    private final int port;

    private SSLServerSocket serverSocket;
    
    private final TreeSet<Player> players = new TreeSet<>();
    private final TreeSet<Player> pendingPlayers = new TreeSet<>();
    
    private final NormalQueue<Player> normalQueue = new NormalQueue<>();
    private final RankedQueue<Player> rankedQueue = new RankedQueue<>();

    private final ExecutorService normalPool = Executors.newVirtualThreadPerTaskExecutor();
    private final ExecutorService rankedPool = Executors.newVirtualThreadPerTaskExecutor();

    private long lastPing = System.currentTimeMillis();
    private final int PING_INTERVAL = 10000;    // Ping players every 10 seconds

    private final int MAX_PING_COUNT = 2;

    public Server(int port) {
        this.port = port;
    }

    public static void main(String[] args) {
        Server server = new Server(8080);

        try {
            server.start();
            server.run();
        } catch (IOException e) {
            System.out.println("Error starting the server: " + e.getMessage());
        } finally {
            try {
                server.stop();
            } catch (IOException e) {
                System.out.println("Error stopping the server: " + e.getMessage());
            }
        }
    } 

    public void start() throws IOException {
        // Set the system properties for the keystore (SSL)
        System.setProperty("javax.net.ssl.keyStore", "key_store.jks");
        System.setProperty("javax.net.ssl.keyStorePassword", "keystore");

        // Create the server socket
        SSLServerSocketFactory factory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
        this.serverSocket = (SSLServerSocket) factory.createServerSocket(this.port);

        Utils.clearScreen();
        System.out.println("Server started on port " + this.port);
    }
    
    public void stop() throws IOException {
        this.serverSocket.close();
    }

    private void run() {
        try {  
            // Start a new thread to ping the players
            Thread.ofVirtual().start(() -> {
                while (true) {
                    try {
                        Thread.sleep(1000);
                        this.pingPlayers();
                    } catch (InterruptedException e) {
                        // Do nothing
                    }
                }
            });

            // Start the queue manager
            Thread.ofVirtual().start(new QueueManager(this.players, this.normalQueue, this.rankedQueue, this.pendingPlayers));
            
            // Start the game scheduler
            Thread.ofVirtual().start(new GameScheduler(this.normalQueue, this.rankedQueue, this.normalPool, this.rankedPool, pendingPlayers));
            
            // Wait for clients to connect
            while (true) {
                SSLSocket clientSocket = (SSLSocket) this.serverSocket.accept();
                System.out.println("Client connected from " + clientSocket.getInetAddress().getHostAddress());
                
                // Start a new thread for the client authentication
                Thread.ofVirtual().start(new ClientAuthenticator(clientSocket, this.players));
            }
        } catch (Exception e) {
            System.out.println("Error running the server: " + e.getMessage());
        }
    }

    private void pingPlayers() {
        if (System.currentTimeMillis() - this.lastPing < PING_INTERVAL) {
            return;
        }

        lastPing = System.currentTimeMillis();

        Iterator<Player> playerIterator = this.players.iterator();
        while (playerIterator.hasNext()) {
            Player player = playerIterator.next();

            // Skip players that are currently playing
            if (player.isPlaying()) {
                continue;
            }

            try {
                System.out.println("Pinging player " + player.getUsername());
                Connection.ping(player.getSocket());
                player.resetPingCount();
            } catch (IOException e) {
                player.incrementPingCount();
                System.out.println("Failed pinging player " + player.getUsername() + " (ping count: " + player.getPingCount() + ")");

                if (player.getPingCount() > MAX_PING_COUNT) {
                    if (this.normalQueue.contains(player)) {
                        this.normalQueue.remove(player);
                        System.out.println("Player " + player.getUsername() + " removed from the normal queue");
                    }

                    if (this.rankedQueue.contains(player)) {
                        this.rankedQueue.remove(player);
                        System.out.println("Player " + player.getUsername() + " removed from the ranked queue");
                    }

                    playerIterator.remove();
                    this.pendingPlayers.remove(player);
                    System.out.println("Player " + player.getUsername() + " removed from the server due to inactivity");
                }
            }
        }
    }
}
