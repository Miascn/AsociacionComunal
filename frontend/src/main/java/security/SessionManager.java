package security;

import java.util.Objects;
import java.util.Optional;

import models.AuthUser;

public final class SessionManager {
    private static final SessionManager INSTANCE = new SessionManager();

    private AuthUser currentUser;
    private String jwtToken;

    private SessionManager() {
    }

    public static SessionManager getInstance() {
        return INSTANCE;
    }

    public void start(AuthUser user, String token) {
        currentUser = Objects.requireNonNull(user, "El usuario de sesión es obligatorio.");
        this.jwtToken = token;
    }

    public Optional<AuthUser> getCurrentUser() {
        return Optional.ofNullable(currentUser);
    }

    public AuthUser requireCurrentUser() {
        return getCurrentUser().orElseThrow(() -> new IllegalStateException("No existe una sesión activa."));
    }

    public String requireToken() {
        if (jwtToken == null || jwtToken.isBlank()) {
            throw new IllegalStateException("No hay un token de sesión activo.");
        }
        return jwtToken;
    }

    public boolean isAuthenticated() {
        return currentUser != null && jwtToken != null;
    }

    public void clear() {
        currentUser = null;
        jwtToken = null;
    }
}