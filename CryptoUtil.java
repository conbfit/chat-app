import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.NamedParameterSpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;



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
        if (salt == null) {
            salt = new byte[32];
        }
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec saltKey = new SecretKeySpec(salt, "HmacSHA256");
        mac.init(saltKey);
        byte[] prk = mac.doFinal(ikm);
        ByteArrayOutputStream okm = new ByteArrayOutputStream();
        byte[] previousT = new byte[0];
        int iterations = (int) Math.ceil((double) length / mac.getMacLength());
        for (int i = 1; i <= iterations; i++) {
            mac.init(new SecretKeySpec(prk, "HmacSHA256"));
            mac.update(previousT);
            if (info != null) {
                mac.update(info);
            }
            mac.update((byte) i);
            previousT = mac.doFinal();
            okm.write(previousT, 0, previousT.length);
        }
        byte[] output = okm.toByteArray();
        return java.util.Arrays.copyOfRange(output, 0, length);
    }
    public static byte[] deriveAesKeyFromSharedSecret(byte[] shared) throws GeneralSecurityException {
        byte[] info = HKDF_INFO.getBytes(StandardCharsets.UTF_8);
        return hkdfSha256(shared, null, info, 32);
    }
    public static String aesGcmEncryptBase64(byte[] aesKey, String plaintext) throws GeneralSecurityException {
    byte[] iv = new byte[12]; rng.nextBytes(iv);
    Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
    GCMParameterSpec spec = new GCMParameterSpec(128, iv);
    SecretKeySpec keySpec = new SecretKeySpec(aesKey, "AES");
    cipher.init(Cipher.ENCRYPT_MODE, keySpec, spec);
    byte[] ct = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    try {
        out.write(iv);
    } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
    }
    try {
        out.write(ct);
    } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
    }
    return Base64.getEncoder().encodeToString(out.toByteArray());
  }

  public static String aesGcmDecryptBase64(byte[] aesKey, String b64) throws GeneralSecurityException {
    byte[] blob = Base64.getDecoder().decode(b64);
    if (blob.length < 13) throw new GeneralSecurityException("ciphertext too short");
    byte[] iv = Arrays.copyOfRange(blob, 0, 12);
    byte[] ct = Arrays.copyOfRange(blob, 12, blob.length);
    Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
    GCMParameterSpec spec = new GCMParameterSpec(128, iv);
    SecretKeySpec keySpec = new SecretKeySpec(aesKey, "AES");
    cipher.init(Cipher.DECRYPT_MODE, keySpec, spec);
    byte[] pt = cipher.doFinal(ct);
    return new String(pt, StandardCharsets.UTF_8);
  }

  public static PublicKey decodePeerPublicKey(String b64) throws GeneralSecurityException {
    return publicKeyFromBase64X25519(b64);
  }

  public static byte[] deriveAesKeyFromKeypair(PrivateKey ourPriv, PublicKey theirPub) throws GeneralSecurityException {
    byte[] shared = deriveSharedSecretX25519(ourPriv, theirPub);
    return deriveAesKeyFromSharedSecret(shared);
  }
}
