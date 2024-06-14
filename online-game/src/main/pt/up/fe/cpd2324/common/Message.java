package pt.up.fe.cpd2324.common;

// Represents a message that can be sent between the client and server
public class Message {
    public enum Type { 
        PLAIN,
        OK,
        ERROR,
        INFO,
        PING,
        SHOW,
        PROMPT,
        USERNAME,
        PASSWORD,
        TOKEN,
        MODE,
        QUEUE,
        GAME,
        CLEAR,
        GAME_OVER,
        END,
    } 

    private final Type type;
    private final String content;

    public Message(Type type, String content) {
        this.type = type;
        this.content = content;
    }

    public Type getType() {
        return this.type;
    }

    public String getContent() {
        return this.content == null ? "" : this.content;
    }

    public static Message fromString(String message) {
        try {
            String[] parts = message.split(": ", 2);
            return new Message(Type.valueOf(parts[0]), parts[1]);
        } catch (NullPointerException e) {
            // Ignore
        }
        return new Message(Type.PLAIN, message);
    }
        
    @Override
    public String toString() {
        return this.type + ": " + this.content;
    }
}
