package pt.up.fe.cpd2324.server;

import pt.up.fe.cpd2324.client.Player;

import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.locks.ReentrantLock;
import java.io.File;
import java.io.IOException;
import java.io.FileWriter;
import java.util.Scanner;
import java.security.SecureRandom;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Database {
    private final static String PATH = "database/db.csv";
    private final Set<Player> players = new TreeSet<>();
    private static final Database instance = new Database();

    private final ReentrantLock lock = new ReentrantLock();

    public static Database getInstance() {
        return instance;
    }
    
    private Database() {
        File folder = new File("database");
        if (!folder.exists()) {
            folder.mkdir();
        }
        
        File file = new File(PATH);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                System.out.println("Error creating the database file: " + e.getMessage());
            }
        }

        this.load();
    }
        
    private void load() {
        this.lock.lock();
        try {
            Scanner scanner = new Scanner(new File(PATH));
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                String[] parts = line.split(";");
                this.players.add(new Player(parts[0], parts[1], parts[2], Integer.parseInt(parts[3])));
            }
            scanner.close();
        } catch (IOException e) {
            System.out.println("Error loading the database: " + e.getMessage());
        } finally {
            this.lock.unlock();
        }
    }

    public void save() {
        this.lock.lock();
        try {
            FileWriter writer = new FileWriter(PATH);
            for (Player player : this.players) {
                String separator = ";";
                writer.write(player.getUsername() + separator + player.getPassword() + separator + player.getSalt() + separator + player.getRating() + "\n");
            }
            writer.close();
        } catch (IOException e) {
            System.out.println("Error saving the database: " + e.getMessage());
        } finally {
            this.lock.unlock();
        }
    }

    public boolean addPlayer(String username, String password) {
        this.lock.lock();
        try {
            String salt = generateSalt();
            String hashedPassword = hashPassword(password, salt);
            
            boolean exists = this.playerExists(username);

            if (!exists) {
                Player player = new Player(username, hashedPassword, salt);
                this.players.add(player);
            
                this.save();
            }

            return !exists;
        } catch (Exception e) {
            System.out.println("Error adding player to the database: " + e.getMessage());
            return false;
        } finally {
            this.lock.unlock();
        }
    }

    public Player getPlayer(String username) {
        this.lock.lock();
        try {
            for (Player player : this.players) {
                if (player.getUsername().equals(username)) {
                    return player;
                }
            }
            return null;
        } catch (Exception e) {
            System.out.println("Error getting player from the database: " + e.getMessage());
            return null;
        } finally {
            this.lock.unlock();
        }
    }

    public boolean playerExists(String username) {
        return this.getPlayer(username) != null;
    }

    public boolean checkPassword(String username, String password) {
        if (!this.playerExists(username)) {
            return false;
        }

        Player player = this.getPlayer(username);
        String hashedPassword = hashPassword(password, player.getSalt());
        
        return player.getPassword().equals(hashedPassword);
    }

    public static String generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16];
        random.nextBytes(salt);

        return bytesToHex(salt);
    }

    public static String hashPassword(String password, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt.getBytes());
            byte[] bytes = md.digest(password.getBytes());
            
            return bytesToHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        
        return null;
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        
        return sb.toString();
    }
    
    public void updateRating(String username, int rating) {
        Player player = this.getPlayer(username);
        player.setRating(rating);
        this.save();
    }
}
