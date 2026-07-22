package services.auth;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import models.AuthResult;
import models.AuthUser;

public class AuthService {
    private static final int MAX_ATTEMPTS = 5;
    private static final long LOCKOUT_MILLIS = 30_000L;

    private final MockAuthRepository repository;
    private final Map<String, Integer> failedAttempts = new HashMap<>();
    private final Map<String, Long> lockedUntil = new HashMap<>();

    public AuthService() {
        this(new MockAuthRepository());
    }

    public AuthService(MockAuthRepository repository) {
        this.repository = repository;
    }

    public AuthResult authenticate(String username, String password) {
        String normalizedUsername = normalize(username);

        if (isLocked(normalizedUsername)) {
            return AuthResult.failure("Cuenta temporalmente bloqueada por demasiados intentos. Intenta de nuevo en unos segundos.");
        }

        Optional<AuthUser> userOpt = repository.findByUsername(normalizedUsername);
        if (userOpt.isEmpty()) {
            registerFailure(normalizedUsername);
            return AuthResult.failure("Usuario o contraseña incorrectos.");
        }

        AuthUser user = userOpt.get();
        boolean validPassword = PasswordHasher.verifyPassword(password, user.getPasswordHash(), user.getSaltHex());
        if (!validPassword) {
            registerFailure(normalizedUsername);
            return AuthResult.failure("Usuario o contraseña incorrectos.");
        }

        failedAttempts.remove(normalizedUsername);
        lockedUntil.remove(normalizedUsername);
        return AuthResult.success(user);
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
