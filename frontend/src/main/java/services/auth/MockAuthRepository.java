package services.auth;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import models.AuthUser;

public class MockAuthRepository {
    private final List<AuthUser> users = new ArrayList<>();

    public MockAuthRepository() {
        registerUser("admin", "Josué Romero", "Administrador", "Admin2026!");
        registerUser("secretaria", "María López", "Secretaria", "Secretaria2026!");
        registerUser("tesorero", "Carlos Vega", "Tesorero", "Tesorero2026!");
    }

    public Optional<AuthUser> findByUsername(String username) {
        String normalized = username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
        return users.stream()
                .filter(user -> user.getUsername().equals(normalized))
                .findFirst();
    }

    private void registerUser(String username, String displayName, String role, String rawPassword) {
        byte[] salt = PasswordHasher.generateSalt();
        String hash = PasswordHasher.hashPassword(rawPassword, salt);
        users.add(new AuthUser(username, displayName, role, hash, PasswordHasher.bytesToHex(salt)));
    }
}
