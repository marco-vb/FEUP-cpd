package pt.up.fe.cpd2324.client;

import pt.up.fe.cpd2324.queue.Rateable;

import java.util.UUID;
import javax.net.ssl.SSLSocket;

public class Player implements Comparable<Player>, Rateable {
    private final String username;
    private final String password;
    private final String salt;
    private int rating;
    private int pingCount;
    private String token;

    private SSLSocket socket;

    private boolean isPlaying = false;

    public Player(String username, String password, String salt) {
        this(username, password, salt, 1000);
    }

    public Player(String username, String password, String salt, int rating) {
        this.username = username;
        this.password = password;
        this.salt = salt;
        this.rating = rating;
        this.pingCount = 0;
    }

    public String getUsername() {
        return this.username;
    }

    public String getPassword() {
        return this.password;
    }

    public String getSalt() {
        return this.salt;
    }

    public int getRating() {
        return this.rating;
    }


    public String getToken() {
        return this.token;
    }

    public SSLSocket getSocket() {
        return this.socket;
    }

    public boolean isPlaying() {
        return this.isPlaying;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public void setSocket(SSLSocket socket) {
        this.socket = socket;
    }

    public void setPlaying(boolean isPlaying) {
        this.isPlaying = isPlaying;
    }

    public int getPingCount() {
        return this.pingCount;
    }

    public void resetPingCount() {
        this.pingCount = 0;
    }

    public void incrementPingCount() {
        this.pingCount++;
    }

    public String generateToken() {
        UUID uuid = UUID.randomUUID();
        this.token = uuid.toString();

        return this.token;
    }

    @Override
    public int compareTo(Player other) {
        if (this.rating != other.rating) {
            return Integer.compare(this.rating, other.rating);
        } else {
            return this.username.compareTo(other.username);
        }
    }
}
