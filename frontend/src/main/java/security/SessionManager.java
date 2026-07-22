package security;

import java.util.Objects;
import java.util.Optional;

import models.AuthUser;

public final class SessionManager {
    private static final SessionManager INSTANCE = new SessionManager();

    private AuthUser currentUser;

    private SessionManager() {
    }

    public static SessionManager getInstance() {
        return INSTANCE;
    }

    public void start(AuthUser user) {
        currentUser = Objects.requireNonNull(user, "El usuario de sesión es obligatorio.");
    }

    public Optional<AuthUser> getCurrentUser() {
        return Optional.ofNullable(currentUser);
    }

    public AuthUser requireCurrentUser() {
        return getCurrentUser().orElseThrow(() -> new IllegalStateException("No existe una sesión activa."));
    }

    public boolean isAuthenticated() {
        return currentUser != null;
    }

    public void clear() {
        currentUser = null;
    }
}
