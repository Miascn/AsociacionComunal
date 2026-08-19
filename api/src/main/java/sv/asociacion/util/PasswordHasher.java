package sv.asociacion.util;

import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.HexFormat;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public final class PasswordHasher {
    private static final int SALT_LENGTH = 16;
    private static final int ITERATIONS = 120_000;
    private static final int KEY_LENGTH = 256;

    private PasswordHasher() {}

    public static String hash(String password) {
        byte[] salt = generateSalt();
        String hash = pbkdf2(password, salt);
        return bytesToHex(salt) + ":" + hash;
    }

    public static boolean verify(String password, String stored) {
        String[] parts = stored.split(":", 2);
        if (parts.length != 2) return false;
        byte[] salt = hexToBytes(parts[0]);
        String expectedHash = parts[1];
        String computedHash = pbkdf2(password, salt);
        return computedHash.equalsIgnoreCase(expectedHash);
    }

    private static String pbkdf2(String password, byte[] salt) {
        try {
            KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] hash = factory.generateSecret(spec).getEncoded();
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new IllegalStateException("No fue posible generar el hash.", e);
        }
    }

    private static byte[] generateSalt() {
        byte[] salt = new byte[SALT_LENGTH];
        new SecureRandom().nextBytes(salt);
        return salt;
    }

    private static String bytesToHex(byte[] bytes) {
        return HexFormat.of().formatHex(bytes);
    }

    private static byte[] hexToBytes(String hex) {
        return HexFormat.of().parseHex(hex);
    }
}