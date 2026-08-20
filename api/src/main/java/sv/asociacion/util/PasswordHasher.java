package sv.asociacion.util;

import java.security.SecureRandom;
import java.security.MessageDigest;
import java.security.spec.KeySpec;
import java.util.Base64;
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
        if (password == null || stored == null) return false;
        if (stored.startsWith("pbkdf2_sha256$")) {
            return verifyModular(password, stored);
        }
        return verifyLegacy(password, stored);
    }

    private static boolean verifyLegacy(String password, String stored) {
        try {
        String[] parts = stored.split(":", 2);
        if (parts.length != 2) return false;
        byte[] salt = hexToBytes(parts[0]);
        String expectedHash = parts[1];
        String computedHash = pbkdf2(password, salt);
        return computedHash.equalsIgnoreCase(expectedHash);
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private static boolean verifyModular(String password, String stored) {
        try {
            String[] parts = stored.split("\\$");
            if (parts.length != 4) return false;
            int iterations = Integer.parseInt(parts[1]);
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expected = Base64.getDecoder().decode(parts[3]);
            KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, KEY_LENGTH);
            byte[] actual = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                .generateSecret(spec).getEncoded();
            return MessageDigest.isEqual(expected, actual);
        } catch (Exception exception) {
            return false;
        }
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
