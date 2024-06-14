package pt.up.fe.cpd2324.server;

import pt.up.fe.cpd2324.common.Connection;
import pt.up.fe.cpd2324.common.Message;
import pt.up.fe.cpd2324.common.TreeSet;
import pt.up.fe.cpd2324.client.Player;

import java.io.IOException;

import javax.net.ssl.SSLSocket;

// Authenticates the client and adds them to the list of authenticated players
public class ClientAuthenticator implements Runnable {
    private final SSLSocket clientSocket;

    private final TreeSet<Player> players;

    private final Database database = Database.getInstance();

    public ClientAuthenticator(SSLSocket socket, TreeSet<Player> players) {
        this.clientSocket = socket;
        this.players = players;
    }

    @Override
    public void run() {
        boolean authenticated = false;

        try {
            while (!authenticated) {
                String[] menu = {
                    " ______________________________",
                    "|                              |",
                    "|  Authentication              |",
                    "|                              |",
                    "|  1. Login                    |",
                    "|  2. Register                 |",
                    "|  3. Reconnect                |",
                    "|                              |",
                    "|  0. Exit                     |",
                    "|                              |",
                    "|______________________________|"
                };
                Connection.show(this.clientSocket, menu);
                
                Connection.prompt(this.clientSocket, "Option: ");
                String option = Connection.receive(this.clientSocket).getContent();

                if (!option.equals("1") && !option.equals("2") && !option.equals("3") && !option.equals("0")) {
                    Connection.error(this.clientSocket, "Invalid option!");
                    continue;
                }

                // Exit
                if (option.equals("0")) {
                    Connection.send(this.clientSocket, new Message(Message.Type.END, null));
                    Connection.close(this.clientSocket);
                    return;
                }

                if (option.equals("3")) {
                    authenticated = this.reconnect();
                    continue;
                }

                Connection.info(this.clientSocket, "Enter 'back' to return to the previous menu");

                Connection.send(this.clientSocket, new Message(Message.Type.USERNAME, "Username: "));
                String username = Connection.receive(this.clientSocket).getContent();

                if (username.equals("back")) {
                    Connection.clear(this.clientSocket);
                    continue;
                }

                Connection.send(this.clientSocket, new Message(Message.Type.PASSWORD, "Password: "));
                String password = Connection.receive(this.clientSocket).getContent();

                if (password.equals("back")) {
                    Connection.clear(this.clientSocket);
                    continue;
                }

                if (username.isEmpty() || password.isEmpty()) {
                    Connection.error(this.clientSocket, "Username and password cannot be empty!");
                    continue;
                }

                switch (option) {
                    case "1":
                        authenticated = this.login(username, password);
                        break;
                    case "2":
                        authenticated = this.register(username, password);
                        break;
                };
            }   
        } catch (IOException | NullPointerException e) {
            // Do nothing
        }
    }

    private boolean login(String username, String password) throws IOException, NullPointerException {
        if (this.database.checkPassword(username, password)) {
            Player player = this.database.getPlayer(username);
            if (this.players.contains(player)) {
                Connection.error(this.clientSocket, "Player already authenticated!");
                return false;
            }

            Connection.ok(this.clientSocket, "Welcome back, " + username + "!");
            
            this.authenticatePlayer(player);
            
            return true;
        } else {
            Connection.error(this.clientSocket, "Invalid username or password!");
            
            return false;
        }
    }
            
    private boolean register(String username, String password) throws IOException, NullPointerException {
        if (this.database.addPlayer(username, password)) {
            Player player = this.database.getPlayer(username);
            
            Connection.ok(this.clientSocket, "Welcome, " + username + "!");
            this.authenticatePlayer(player);
            return true;
        } else {
            Connection.error(this.clientSocket, "Username already taken!");
            return false;
        }
    }

    private void authenticatePlayer(Player player) throws IOException {
        System.out.println("Player " + player.getUsername() + " authenticated");

        player.setSocket(this.clientSocket);
        this.players.add(player);

        // Send the player their token
        Connection.send(this.clientSocket, new Message(Message.Type.TOKEN, player.getUsername() + "--" + player.generateToken()));
    }

    private boolean reconnect() throws IOException, NullPointerException {
        Connection.send(this.clientSocket, new Message(Message.Type.TOKEN, null));
        String token = Connection.receive(this.clientSocket).getContent();

        for (Player player : this.players) {
            if (player.getToken().equals(token)) {
                if (player.isPlaying()) {
                    Connection.error(this.clientSocket, "Try again later!");
                    return false;
                }

                // Update the player's socket to the new one
                Connection.close(player.getSocket());
                player.setSocket(this.clientSocket);

                Connection.ok(this.clientSocket, "Welcome back, " + player.getUsername() + "!");
                Connection.send(this.clientSocket, new Message(Message.Type.TOKEN, player.getUsername() + "--" + player.generateToken()));
               
                return true;
            }
        }

        Connection.error(this.clientSocket, "Invalid token!");
        
        return false;
    }
}
