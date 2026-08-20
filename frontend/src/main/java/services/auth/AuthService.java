package services.auth;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import models.AuthResult;
import models.AuthUser;
import service.AuthApiClient;
import service.AuthApiClient.LoginResponse;

public class AuthService {
    private static final int MAX_ATTEMPTS = 5;
    private static final long LOCKOUT_MILLIS = 30_000L;

    private final Map<String, Integer> failedAttempts = new HashMap<>();
    private final Map<String, Long> lockedUntil = new HashMap<>();

    public AuthResult authenticate(String username, String password) {
        String normalizedUsername = normalize(username);

        if (isLocked(normalizedUsername)) {
            return AuthResult.failure("Cuenta temporalmente bloqueada por demasiados intentos. Intenta de nuevo en unos segundos.");
        }

        try {
            AuthApiClient client = new AuthApiClient();
            LoginResponse response = client.login(normalizedUsername, password);
            failedAttempts.remove(normalizedUsername);
            lockedUntil.remove(normalizedUsername);
            AuthUser user = new AuthUser(normalizedUsername, response.displayName(), response.role());
            return AuthResult.success(user, response.token());
        } catch (Exception e) {
            registerFailure(normalizedUsername);
            return AuthResult.failure(e.getMessage());
        }
    }

    private boolean isLocked(String username) {
        Long until = lockedUntil.get(username);
        if (until == null) {
            return false;
        }
        if (System.currentTimeMillis() > until) {
            lockedUntil.remove(username);
            failedAttempts.remove(username);
            return false;
        }
        return true;
    }

    private void registerFailure(String username) {
        int attempts = failedAttempts.getOrDefault(username, 0) + 1;
        failedAttempts.put(username, attempts);
        if (attempts >= MAX_ATTEMPTS) {
            lockedUntil.put(username, System.currentTimeMillis() + LOCKOUT_MILLIS);
        }
    }

    private String normalize(String username) {
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    }
}