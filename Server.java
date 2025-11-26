import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
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

    /**
     * Broadcast an encrypted message to all connections except the provided one.
     * If encryption fails for a recipient, fall back to plaintext for that recipient.
     */
    public void broadcastEncryptedExcept(ConnectionHandler exclude, String message) {
        for (ConnectionHandler handler : connections) {
            if (handler == null || handler == exclude) continue;
            try {
                handler.sendEncrypted(message);
            } catch (GeneralSecurityException e) {
                handler.sendMessage(message);
            }
        }
    }
    public void shutdown() {
        try {
            done = true;
            if (server != null && !server.isClosed()) {
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

    


    class ConnectionHandler implements Runnable {

        private Socket client;
        private BufferedReader in;
        private PrintWriter out;
        private String nickname;
        private byte[] sessionKey;
        private PrivateKey dhPrivateKey;

        public ConnectionHandler(Socket client) {
            this.client = client;
        }


        @Override
        public void run() {
            try {
                out = new PrintWriter(client.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                
                // Perform Diffie-Hellman key exchange
                String clientInitMsg = in.readLine();
                if (clientInitMsg != null && clientInitMsg.startsWith("DHINIT:")) {
                    String clientPubKeyB64 = clientInitMsg.substring("DHINIT:".length());
                    PublicKey clientPubKey = CryptoUtil.publicKeyFromBase64X25519(clientPubKeyB64);
                    
                    KeyPair serverKp = CryptoUtil.generateKeyPair();
                    dhPrivateKey = serverKp.getPrivate();
                    String serverPubKeyB64 = CryptoUtil.publicKeyToBase64(serverKp.getPublic());
                    out.println("DHRESP:" + serverPubKeyB64);
                    
                    sessionKey = CryptoUtil.deriveAesKeyFromKeypair(dhPrivateKey, clientPubKey);
                    System.out.println("DH handshake completed with client.");
                }
                
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
                        // If message is encrypted from this client, decrypt it using this handler's sessionKey
                        String plaintext = message;
                        if (message.startsWith("ENC:") && sessionKey != null) {
                            try {
                                String b64 = message.substring("ENC:".length());
                                plaintext = CryptoUtil.aesGcmDecryptBase64(sessionKey, b64);
                            } catch (GeneralSecurityException e) {
                                // If decryption fails, keep original message so it's not lost
                                plaintext = "[unreadable message]";
                            }
                        }

                        // Broadcast plaintext to every connection, encrypting for recipients that have a session key
                        for (ConnectionHandler handler : connections) {
                            try {
                                handler.sendEncrypted(nickname + ": " + plaintext);
                            } catch (GeneralSecurityException e) {
                                // Fallback to plain send if encryption fails for a recipient
                                handler.sendMessage(nickname + ": " + plaintext);
                            }
                        }
                    }
                }

            } catch (IOException | GeneralSecurityException e) {
                shutdown();
            }

        }
        public void sendMessage(String message) {
            out.println(message);
        }

        /**
         * Send a message to this handler, encrypting it with this handler's session key if available.
         */
        public void sendEncrypted(String message) throws GeneralSecurityException {
            if (sessionKey == null) {
                out.println(message);
            } else {
                String enc = "ENC:" + CryptoUtil.aesGcmEncryptBase64(sessionKey, message);
                out.println(enc);
            }
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
