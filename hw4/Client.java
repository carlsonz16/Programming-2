import java.net.*;
import java.io.*;
import java.util.Scanner;

public class Client {
    private String hostname;
    private int port;
    
    public Client(String hostname, int port) {
        this.hostname = hostname;
        this.port = port;
    }
    public void execute() {
        try {
            Socket socket = new Socket(hostname, port);
            System.out.println("Connected to the server");
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
            
            try (Scanner scanner = new Scanner(System.in)) {
                
                // Receive list of online users
                String response = reader.readLine();
                System.out.println(response);
                
                // Receive last 2 messages
                // ...
                
                // Start a new thread to listen for incoming messages
                Thread thread = new Thread(new IncomingReader(reader));
                thread.start();
                
                // Send messages to the server
                String message;
                do {
                    message = scanner.nextLine();
                    writer.println(message);
                } while (!message.equals("bye"));
            }
            socket.close();
        } catch (IOException ex) {
            System.err.println("Error connecting to server: " + ex.getMessage());
        }
    }
    
    public static void main(String[] args) {
        Client client = new Client("localhost", 9001);
        client.execute();
    }
    
    class IncomingReader implements Runnable {
        private BufferedReader reader;
        
        public IncomingReader(BufferedReader reader) {
            this.reader = reader;
        }
        
        public void run() {
            String message;
            try {
                while ((message = reader.readLine()) != null) {
                    System.out.println(message);
                }
            } catch (IOException ex) {
                System.err.println("Error reading incoming message: " + ex.getMessage());
            }
        }
    }
}
