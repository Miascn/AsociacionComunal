package models;

import java.util.Locale;

public class AuthUser {
    private final String username;
    private final String displayName;
    private final String role;
    private final String passwordHash;
    private final String saltHex;

    public AuthUser(String username, String displayName, String role, String passwordHash, String saltHex) {
        this.username = username.toLowerCase(Locale.ROOT);
        this.displayName = displayName;
        this.role = role;
        this.passwordHash = passwordHash;
        this.saltHex = saltHex;
    }

    public String getUsername() {
        return username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getRole() {
        return role;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getSaltHex() {
        return saltHex;
    }
}
