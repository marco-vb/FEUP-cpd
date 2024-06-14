package pt.up.fe.cpd2324.client;

import pt.up.fe.cpd2324.common.Connection;
import pt.up.fe.cpd2324.common.Message;
import pt.up.fe.cpd2324.common.Utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

public class Client {
    private final int port;
    private final String hostname;
    private SSLSocket socket;
    
    private String username;

    private final static String TOKEN_PATH = "src/main/pt/up/fe/cpd2324/client/tokens/";

    private Client(String hostname, int port) {
        this.hostname = hostname;
        this.port = port;
    }

    private Client (String hostname, int port, String username) {
        this.hostname = hostname;
        this.port = port;
        this.username = username;
    }

    public static void main(String[] args) {
        Client client;

        // If the username is provided as an argument, use it
        if (args.length == 1) {
            client = new Client("localhost", 8080, args[0]);
        } else {
            client = new Client("localhost", 8080);
        }

        try {
            client.start();
            client.run();
        } catch (IOException e) {
            System.out.println("Error starting the client: " + e.getMessage());
        } finally {
            try {
                client.stop();
            } catch (IOException e) {
                System.out.println("Error stopping the client: " + e.getMessage());
            }
        }
    }

    private void start() throws IOException {
        Utils.clearScreen();

        // Set the system properties for the keystore (SSL)
        System.setProperty("javax.net.ssl.trustStore", "key_store.jks");
        System.setProperty("javax.net.ssl.trustStorePassword", "keystore");

        // Create the client socket
        SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
        this.socket = (SSLSocket) factory.createSocket(this.hostname, this.port);
    }

    private void stop() throws IOException {
        Utils.clearScreen();
        System.out.println("Exiting...");
        if (this.socket != null) {
            this.socket.close();
        }
    }

    private void run() {
        try {
            if (this.authenticate()) {  // Authenticate the user, if successful, start listening
                this.listen();
            } else {
                Utils.clearScreen();
                System.out.println("Exiting...");
                this.stop();
            }
        } catch (IOException e) {
            System.out.println("Error running the client: " + e.getMessage());
        }
    }

    private boolean authenticate() throws IOException {
        while (true) {
            Message message = Connection.receive(this.socket);
            String content = message.getContent();

            switch (message.getType()) {
                case SHOW:
                    System.out.println(content);
                    break;
                case CLEAR:
                    Utils.clearScreen();
                    break;
                case INFO:
                    Utils.clearScreen();
                    System.out.println(content);
                    break;
                case PROMPT:
                    System.out.println();
                    Connection.send(this.socket, System.console().readLine(content));
                    break;
                case USERNAME:
                    System.out.println();
                    Connection.send(this.socket, System.console().readLine(content));
                    break;
                case PASSWORD:
                    System.out.println();
                    Connection.send(this.socket, new String(System.console().readPassword(content)));
                    break;
                case TOKEN:
                    Connection.send(this.socket, this.readToken());
                    break;
                case OK:
                    Utils.clearScreen();
                    this.writeToken(Connection.receive(this.socket).getContent());
                    return true;
                case ERROR:
                    Utils.clearScreen();
                    System.out.println(content);
                    break;
                case END:
                    this.stop();
                    return false;
                default:
                    System.out.println("Invalid message type in authenticate: " + message.getType());
                    break;
            }
        }
    }

    // Listen for messages from the server and handle them
    private void listen() throws IOException {
        while (true) {
            Message message = Connection.receive(this.socket);

            switch (message.getType()) {    // 2 possible states: Select game mode or play game
                case MODE:
                    this.selectGameMode();
                    break;
                case GAME:
                    this.playGame();
                    break;
                case PING:  // Ignore pings from the server
                    break;
                case INFO:
                    Utils.clearScreen();
                    System.out.println(message.getContent());
                    System.out.println(Connection.receive(this.socket).getContent());
                    break;
                case CLEAR:
                    Utils.clearScreen();
                    break;
                default:
                    System.out.println("Invalid message type in listen: " + message.getType());
            }
        }
    }

    private void selectGameMode() throws IOException {
        while (true) { 
            Message message = Connection.receive(this.socket);
            String content = message.getContent();

            switch (message.getType()) {
                case MODE:
                    Utils.clearScreen();
                case SHOW:
                    System.out.println(content);
                    break;
                case PROMPT: 
                    System.out.println();
                    Connection.send(this.socket, System.console().readLine(content));
                    break;
                case OK:
                    return;
                case ERROR:
                    Utils.clearScreen();
                    System.out.println(content);
                    break;
                case PING:
                    break;
                case PLAIN:
                    System.out.println(content);
                    break;
                case END:
                    this.stop();
                    return;
                default:
                    System.out.println("Invalid message type in selectGameMode: " + message.getType());
            }
        }
    }

    private void playGame() throws IOException {
        while (true) {
            Message message = Connection.receive(this.socket);
            String content = message.getContent();

            switch (message.getType()) {
                case SHOW:
                    System.out.println(content);
                    break;
                case PROMPT:
                    System.out.println();
                    Connection.send(this.socket, System.console().readLine(content));
                    break;
                case CLEAR:
                    Utils.clearScreen();
                    break;
                case INFO:
                    System.out.println();
                    System.out.println(content);
                    break;  
                case GAME_OVER:
                    Utils.clearScreen();
                    System.out.println(content);
                    System.out.println();
                    return;
                case ERROR:
                    Utils.clearScreen();
                    System.out.println(content);
                    break;
                case MODE:
                    break;
                default:
                    System.out.println("Invalid message type in playGame: " + message.getType());
            }
        }
    }

    private void writeToken(String reponse) {
        String[] parts = reponse.split("--");  // Split the response into "username--token
        String username = parts[0];
        String token = parts[1];

        File folder = new File(TOKEN_PATH);
        
        if (!folder.exists()) {
            folder.mkdir();
        }
        
        File file = new File(TOKEN_PATH + username + ".txt");
        try {
            FileWriter writer = new FileWriter(file);
            writer.write(token);
            writer.close();
        } catch (IOException e) {
            System.out.println("Error writing the token: " + e.getMessage());
        }
    }

    private String readToken() {      
        if (this.username == null) {
            return "";
        }

        File file = new File(TOKEN_PATH + this.username + ".txt");
        try {
            Scanner scanner = new Scanner(file);
            String token = scanner.nextLine();
            scanner.close();
            return token;
        } catch (IOException e) {
            System.out.println("Error reading the token: " + e.getMessage());
        }

        return "";
    }
}