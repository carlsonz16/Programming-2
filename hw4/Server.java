import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class Server {
    private int port;
    private Set<String> userNames = new HashSet<>();
    private Set<UserThread> userThreads = new HashSet<>();
    public int messageId = 0;
    private List<String> messages = new ArrayList<>();

    public Server(int port) {
        this.port = port;
    }

    public void execute() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server is listening on port " + port);

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("New user connected");

                UserThread newUser = new UserThread(socket, this);
                userThreads.add(newUser);
                newUser.start();
            }
        } catch (IOException ex) {
            System.out.println("Error in the server: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    public static void main(String[] args) {
        

        Server server = new Server(9001);
        server.execute();
    }

    void addUserName(String userName) {
        userNames.add(userName);
    }

    void removeUser(String userName, UserThread userThread) {
        boolean removed = userNames.remove(userName);
        if (removed) {
            userThreads.remove(userThread);
            System.out.println(userName + " has left the group");
            broadcast(userName + " has left the group");
        }
    }

    Set<String> getUserNames() {
        return this.userNames;
    }

    boolean hasUsers() {
        return !this.userNames.isEmpty();
    }

    void addMessage(String message) {
        synchronized (this) {
            messageId++;
            String fullMessage = messageId + ", " + message;
            messages.add(fullMessage);
    
            if (messages.size() > 2) {
                messages.remove(0);
            }
            
            System.out.println(fullMessage); // print the full message
        }
    }
    

    List<String> getMessages() {
        return this.messages;
    }

    void broadcast(String message) {
        synchronized (this) {
            for (UserThread userThread : userThreads) {
                userThread.sendMessage(message);
            }
        }
    }

    public int getMessageId() {
        return messageId;
    }

    void sendMessageToUser(String message, String userName) {
        synchronized (this) {
            for (UserThread userThread : userThreads) {
                if (userThread.getUserName().equals(userName)) {
                    userThread.sendMessage(message);
                    break;
                }
            }
        }
    }
}

class UserThread extends Thread {
    private Socket socket;
    private Server server;
    private PrintWriter writer;
    private String userName;

    public UserThread(Socket socket, Server server) {
        this.socket = socket;
        this.server = server;
    }

    public void run() {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream(), true);

            String message;
            String subject;
            // Ask for user name
            do {
                writer.println("Enter username: ");
                userName = reader.readLine();
            } while (userName == null || userName.isEmpty() || server.getUserNames().contains(userName));

            // Welcome the user
            writer.println("Welcome " + userName + " to the group.\n");

            // Notify other users about new user
            String userJoinedMessage = userName + " has joined the group.";
            server.addUserName(userName);
            server.broadcast(userJoinedMessage);

            // Send list of current users to new user
            Set<String> currentUsers = server.getUserNames();
            String usersListMessage = "Current users in the group: " + currentUsers.toString();
            writer.println(usersListMessage);

            // Send last 2 messages to new user
            List<String> messages = server.getMessages();
            if (messages.size() > 0) {
                writer.println("Last 2 messages:");
                for (String msg : messages) {
                    writer.println(msg);
                }
                writer.println();
            }

            // Accept messages from this client and broadcast to others in the group
            while (true) {
                int messageId = server.getMessageId();
                writer.println("Input the subject: ");
                subject = reader.readLine();
                //enter subject line here
                writer.println("Input your message: ");
                message = reader.readLine();
                if (message == null || message.equals("/quit") || subject.equals("/quit")) {
                    break;
                }

            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String formattedDate = dateFormat.format(new Date());
            String fullMessage = messageId + ", "  + message + ", " + formattedDate + ", " + subject;

            server.addMessage(userName + ": " + fullMessage);
            server.broadcast(userName + ": " + fullMessage);
            }

            // User is leaving
            server.removeUser(userName, this);
            socket.close();

            // Notify other users about the user leaving
            String userLeftMessage = userName + " has left the group.";
            server.broadcast(userLeftMessage);
        } catch (IOException ex) {
            System.out.println("Error in UserThread: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    void sendMessage(String message) {
        writer.println(message);
    }

    String getUserName() {
        return this.userName;
    }
}
