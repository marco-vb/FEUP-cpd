package pt.up.fe.cpd2324.server;

import pt.up.fe.cpd2324.common.Connection;
import pt.up.fe.cpd2324.common.Message;
import pt.up.fe.cpd2324.client.Player;
import pt.up.fe.cpd2324.game.Stones;

import java.io.IOException;

// Represents a game between two players
// Handles the game logic and interactions with the players
public class Game implements Runnable {
    private Player currentPlayer;
    private Player otherPlayer;

    private final Boolean ranked;   // Whether the game is ranked or not
    private Player winner;
    private Player loser;
    
    private final Stones stones = new Stones(); // Game state

    private final int TIMEOUT = 60000;  // Time in milliseconds to wait for a player's move

    public Game(Player player1, Player player2, Boolean ranked) {
        this.currentPlayer = player1;
        this.otherPlayer = player2;
        this.ranked = ranked;
    }

    @Override
    public void run() {
        try {
            System.out.println("Starting game between " + this.currentPlayer.getUsername() + " and " + this.otherPlayer.getUsername());
            
            // Notify the players that the game is starting
            Connection.send(this.currentPlayer.getSocket(), new Message(Message.Type.GAME, null));
            Connection.send(this.otherPlayer.getSocket(), new Message(Message.Type.GAME, null));

            // Play the game until it's over
            while (!this.stones.isGameOver()) {
                this.takeTurn();
            }

            // Notify the players of the game result
            Connection.send(this.winner.getSocket(), new Message(Message.Type.GAME_OVER, "You won!"));
            Connection.send(this.loser.getSocket(), new Message(Message.Type.GAME_OVER, "You lost!"));

            System.out.println("Game between " + this.currentPlayer.getUsername() + " and " + this.otherPlayer.getUsername() + " ended");
        } catch (IOException | NullPointerException e) {
            
            // If a player times out, the other player wins
            this.winner = this.otherPlayer;
            this.loser = this.currentPlayer;

            try {
                Connection.send(this.winner.getSocket(), new Message(Message.Type.GAME_OVER, "The other player disconnected! You won!"));
                Connection.send(this.loser.getSocket(), new Message(Message.Type.GAME_OVER, "You ran out of time! You lost!"));
            } catch (IOException e1) {
                // Do nothing
            }
        } finally {
            this.endGame();
        }
    }

    // Switch the current player with the other player
    private void switchPlayers() {
        Player temp = this.currentPlayer;
        this.currentPlayer = this.otherPlayer;
        this.otherPlayer = temp;
    }

    public void takeTurn() throws IOException, NullPointerException {
        // Show the current state of the game to both players
        String[] state = this.stones.toString().split("\n");
        
        Connection.clear(this.currentPlayer.getSocket());
        Connection.clear(this.otherPlayer.getSocket());
        
        if (ranked) {
            this.showRating();
        }

        Connection.show(this.currentPlayer.getSocket(), state);
        Connection.show(this.otherPlayer.getSocket(), state);
        
        Connection.send(this.otherPlayer.getSocket(), new Message(Message.Type.INFO, "Waiting for the other player...")); 
        
        // Process the current player's move
        if (this.processMove()) {
            return;
        }

        this.switchPlayers();
    }

    // Returns true if the game is over, false otherwise
    private boolean processMove() throws IOException, NullPointerException {
        while (true) {
            Connection.prompt(this.currentPlayer.getSocket(), "Enter your move (stack numStones): ");
            
            // Receive the move from the current player
            String move = Connection.receive(this.currentPlayer.getSocket(), TIMEOUT).getContent();

            // If the player quits, end the game, and the other player wins
            if (move.equals("quit")) {
                this.winner = this.otherPlayer;
                this.loser = this.currentPlayer;
                return true;
            }

            try {
                String moveParts[] = move.split(" ");
                
                if (moveParts.length != 2) {
                    throw new ArrayIndexOutOfBoundsException();
                }
                
                int stack = Integer.parseInt(moveParts[0]);
                int numStones = Integer.parseInt(moveParts[1]);

                if (!this.stones.removeStones(stack - 1, numStones)) {
                    continue;
                }

                if (this.stones.isGameOver()) {
                    this.winner = this.currentPlayer;
                    this.loser = this.otherPlayer;

                    return true;
                }

                return false;
            } catch (ArrayIndexOutOfBoundsException | NumberFormatException e) {
                continue;
            }       
        }
    }

    private void endGame() {
        // Update the ratings of the players if the game was ranked
        if (this.ranked) {
            this.updateRatings();
        }

        // Set the players as not playing, so they can join another game
        this.currentPlayer.setPlaying(false);
        this.otherPlayer.setPlaying(false);
    }

    // Calculate the expected score of a player in a game
    private double expectedScore(int rating1, int rating2) {
        return 1 / (1 + Math.pow(10, (rating2 - rating1) / 400.0));
    }
    
    // Update the ratings of the players based on the outcome of the game
    private void updateRatings() { 
        int rating1 = this.winner.getRating();
        int rating2 = this.loser.getRating();
        
        double expected1 = this.expectedScore(rating1, rating2);
        double expected2 = this.expectedScore(rating2, rating1);

        // K-factor for Elo rating system
        int k = 32;
        
        int newRating1 = (int) (rating1 + k * (1 - expected1));
        int newRating2 = (int) (rating2 + k * (0 - expected2));

        this.winner.setRating(newRating1);
        this.loser.setRating(newRating2);

        // Save the new ratings to the database
        Database.getInstance().save();
    }

    // Show the rating of the players to each other
    private void showRating() throws IOException {
        Connection.show(this.currentPlayer.getSocket(), new String[] {
            "Your rating: " + this.currentPlayer.getRating(),
            this.otherPlayer.getUsername() + "'s rating: " + this.otherPlayer.getRating(),
            "\n"
        });

        Connection.show(this.otherPlayer.getSocket(), new String[] {
            "Your rating: " + this.otherPlayer.getRating(),
            this.currentPlayer.getUsername() + "'s rating: " + this.currentPlayer.getRating(),
            "\n"
        });
    }
}
