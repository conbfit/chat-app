import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.NamedParameterSpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import javax.crypto.KeyAgreement;



public class CryptoUtil {
    private static final SecureRandom rng = new SecureRandom();
    private static final String HKDF_INFO = "chat-app";

    public static KeyPair generateKeyPair() throws GeneralSecurityException {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("X25519");
        kpg.initialize(new NamedParameterSpec("X25519"));
        return kpg.generateKeyPair();
    }
    public static String publicKeyToBase64(PublicKey publicKey) {
        return Base64.getEncoder().encodeToString(publicKey.getEncoded());
    }

    public static PublicKey publicKeyFromBase64X25519(String b64) throws GeneralSecurityException {
        byte[] decodedKey = Base64.getDecoder().decode(b64);
        KeyFactory kf = KeyFactory.getInstance("X25519");
        X509EncodedKeySpec spec = new X509EncodedKeySpec(decodedKey);
        return kf.generatePublic(spec);
    }
    public static byte[] deriveSharedSecretX25519(PrivateKey privateKey, PublicKey otherKey) throws GeneralSecurityException {
        KeyAgreement ka = KeyAgreement.getInstance("X25519");
        ka.init(privateKey);
        ka.doPhase(otherKey, true);
        return ka.generateSecret();
    }
    public static byte[] hkdfSha256(byte[] ikm, byte[] salt, byte[] info, int length) throws GeneralSecurityException {
        // HKDF implementation would go here
        return new byte[length]; // Placeholder
    }

}
