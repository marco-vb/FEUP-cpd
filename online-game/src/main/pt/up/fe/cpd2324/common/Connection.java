package pt.up.fe.cpd2324.common;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import javax.net.ssl.SSLSocket;

// Handles sending and receiving messages through a socket
public class Connection {  
    public static void send(SSLSocket socket, Message message) throws IOException {
        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        out.write(message.toString());
        out.newLine();
        out.flush();
    }   
    
    // Send plain text message
    // Overloads send method for convenience (no need to create a Message object)
    public static void send(SSLSocket socket, String message) throws IOException {
        send(socket, new Message(Message.Type.PLAIN, message));
    }
    
    public static Message receive(SSLSocket socket) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        return Message.fromString(in.readLine());   
    }

    public static Message receive(SSLSocket socket, long timeout) throws IOException {
        socket.setSoTimeout((int) timeout);
        try {
            return receive(socket);
        } catch (IOException e) {
            throw new IOException("Timeout");
        } finally {
            socket.setSoTimeout(0);
        }
    }

    public static void ok(SSLSocket socket, String content) throws IOException {
        Connection.send(socket, new Message(Message.Type.OK, content));
    }

    public static void error(SSLSocket socket, String content) throws IOException {
        Connection.send(socket, new Message(Message.Type.ERROR, content));
    }

    public static void ping(SSLSocket socket) throws IOException {
        Connection.send(socket, new Message(Message.Type.PING, null));
    }

    public static void info(SSLSocket socket, String content) throws IOException {
        Connection.send(socket, new Message(Message.Type.INFO, content));
    }

    public static void show(SSLSocket socket, String content) throws IOException {
        Connection.send(socket, new Message(Message.Type.SHOW, content));
    }

    // Send command to clear the screen
    public static void clear(SSLSocket socket) throws IOException {
        Connection.send(socket, new Message(Message.Type.CLEAR, null));
    }
    
    // Send multiple lines of text, each line is a separate message
    // Receiver should handle each message separately
    public static void show(SSLSocket socket, String[] content) throws IOException {
        for (String line : content) {
            Connection.show(socket, line);
        }
    }     

    public static void prompt(SSLSocket socket, String content) throws IOException {
        Connection.send(socket, new Message(Message.Type.PROMPT, content));
    }
    
    public static void close(SSLSocket socket) throws IOException {
        socket.close();
    }
}
