import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server implements Runnable {
    private ArrayList<ConnectionHandler> connections;
    private ServerSocket server;
    private boolean done;
    private ExecutorService pool;


    public Server() {
        connections = new ArrayList<>();
        done = false;
    }


    @Override
    public void run() {
        try {
            server = new ServerSocket(9999);
            pool = Executors.newCachedThreadPool();
            while (!done) {
                Socket client = server.accept();
                ConnectionHandler handler = new ConnectionHandler(client);
                connections.add(handler);
                pool.execute(handler);
            }
        } catch (IOException e) {
            shutdown();
        }
    }

    public void broadcastMessage(String message) {
        for (ConnectionHandler handler : connections) {
            if (handler != null) {
                handler.sendMessage(message);
            }
        }
    }
    public void shutdown() {
        try {
            done = true;
            if (!server.isClosed()) {
                server.close();
            }
            for (ConnectionHandler handler : connections) {
                handler.sendMessage("Server is shutting down...");
                handler.shutdown();
            }
        } catch (IOException e) {
            //ignore
        }
    }

    


    //handles client connections
    class ConnectionHandler implements Runnable {

        private Socket client;
        private BufferedReader in;
        private PrintWriter out;
        private String nickname;

        public ConnectionHandler(Socket client) {
            this.client = client;
        }


        @Override
        public void run() {
            try {
                out = new PrintWriter(client.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                out.println("enter a nickname: ");
                nickname = in.readLine();
                System.out.println(nickname + " has connected.");
                broadcastMessage(nickname + " joined the chat");
                String message;
                while ((message = in.readLine()) != null) {
                    if (message.startsWith("/nick ")) {
                        String[] messageParts = message.split(" ", 2);
                        if (messageParts.length == 2) {
                            broadcastMessage(nickname + " renamed to " + messageParts[1]);
                            nickname = messageParts[1];
                            out.println("Nickname successfully changed to " + nickname);
                        } else {
                            out.println("Invalid nickname command. Usage: /nick <new_nickname>");
                        }

                    } else if (message.startsWith("/quit")) {
                        shutdown();

                    } else {
                        broadcastMessage(nickname + ": " + message);
                    }
                }

            } catch (IOException e) {
                shutdown();
            }

        }
        public void sendMessage(String message) {
            out.println(message);
        }
        public void shutdown() {
            broadcastMessage(nickname + " has disconnected.");

            try {
                in.close();
                out.close(); 
                if (!client.isClosed()) {
                    client.close();
                }
            } catch (IOException e) {
                //ignore
            }
        }
    }
    public static void main(String[] args) {
        Server server = new Server();
        server.run();
    }
}
