package src;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;

public class Client implements Runnable{
    private Socket client;
    private BufferedReader in;
    private PrintWriter out;
    private boolean done;
    private byte[] sessionKey;
    private PrivateKey dhPrivateKey;
@Override
    public void run() {
        try {
            client = new Socket("localhost", 9999);
            out = new PrintWriter(client.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(client.getInputStream()));

            KeyPair clientKp = CryptoUtil.generateKeyPair();
            dhPrivateKey = clientKp.getPrivate();
            String clientPubKeyB64 = CryptoUtil.publicKeyToBase64(clientKp.getPublic());
            out.println("DHINIT:" + clientPubKeyB64);

            String respLine = in.readLine();
            if (respLine != null && respLine.startsWith("DHRESP:")) {
                String serverPubKeyB64 = respLine.substring("DHRESP:".length());
                PublicKey serverPubKey = CryptoUtil.publicKeyFromBase64X25519(serverPubKeyB64);
                
                sessionKey = CryptoUtil.deriveAesKeyFromKeypair(dhPrivateKey, serverPubKey);
                System.out.println("DH handshake completed with server.");
            }
            
            String nicknamePrompt = in.readLine();
            System.out.println(nicknamePrompt);

            BufferedReader consoleIn = new BufferedReader(new InputStreamReader(System.in));
            String nickname = consoleIn.readLine();
            out.println(nickname);
            
            InputHandler inputHandler = new InputHandler(consoleIn);
            Thread t = new Thread(inputHandler);
            t.start();

            String inMessage;
            while ((inMessage = in.readLine()) != null) {
                String decrypted = decryptMessage(inMessage);
                System.out.println(decrypted);
            }
        } catch (IOException | GeneralSecurityException e) {
            shutdown();
        }
    }
    private void sendEncrypted(String plaintext) throws GeneralSecurityException {
    if (sessionKey == null) {
        out.println(plaintext); // fallback
    } else {
        String encrypted = "ENC:" + CryptoUtil.aesGcmEncryptBase64(sessionKey, plaintext);
        out.println(encrypted);
    }
}

private String decryptMessage(String line) throws GeneralSecurityException {
    if (line == null || !line.startsWith("ENC:")) {
        return line; // plaintext fallback
    }
    String b64Ciphertext = line.substring("ENC:".length());
    return CryptoUtil.aesGcmDecryptBase64(sessionKey, b64Ciphertext);
}

    public void shutdown() {
        done = true;
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

    class InputHandler implements Runnable {
        private BufferedReader consoleIn;
        
        public InputHandler(BufferedReader consoleIn) {
            this.consoleIn = consoleIn;
        }
        
        @Override
        public void run() {
            try {
                while (!done) {
                    String message = consoleIn.readLine();
                    if (message.equals("/quit")) {
                        //broadcastMessage(nickname + " has disconnected.");
                        out.println(message);
                        consoleIn.close();
                        shutdown();
                    } else {
                        try {
                            sendEncrypted(message);
                        } catch (GeneralSecurityException e) {
                            e.printStackTrace();
                        }
                    }
                }
            } catch (IOException e) {
                shutdown();

            }
        }
    }
    public static void main(String[] args) {
        Client client = new Client();
        client.run();
    }
}
