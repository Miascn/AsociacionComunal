package sv.asociacion.api.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Set;

public final class AuthService {
    public static final Set<String> SYSTEM_ROLES = Set.of(
        "MIEMBRO", "ADMINISTRADOR", "PRESIDENTE", "SECRETARIO", "TESORERO", "SINDICO"
    );
    private final UserAuthRepository users;
    private final SessionRepository sessions;
    private final PasswordService passwords;
    private final TokenGenerator tokens;
    private final Clock clock;
    private final Duration accessLifetime;
    private final Duration refreshLifetime;

    public AuthService(UserAuthRepository users, SessionRepository sessions) {
        this(users, sessions, new PasswordService(), new SecureTokenGenerator(), Clock.systemUTC(),
            Duration.ofMinutes(readPositive("AUTH_ACCESS_TTL_MINUTES", 15)),
            Duration.ofDays(readPositive("AUTH_REFRESH_TTL_DAYS", 30)));
    }

    public AuthService(UserAuthRepository users, SessionRepository sessions, PasswordService passwords,
                       TokenGenerator tokens, Clock clock, Duration accessLifetime, Duration refreshLifetime) {
        this.users = users; this.sessions = sessions; this.passwords = passwords; this.tokens = tokens;
        this.clock = clock; this.accessLifetime = accessLifetime; this.refreshLifetime = refreshLifetime;
    }

    public LoginResult login(String username, String password) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            throw new AuthException(400, "INVALID_REQUEST", "Usuario y contraseña son obligatorios.");
        }
        AuthUser user = users.findByUsername(username.trim().toLowerCase(Locale.ROOT))
            .orElseThrow(AuthService::invalidCredentials);
        if (!passwords.verify(password, user.passwordHash())) throw invalidCredentials();
        validateActiveIdentity(user);
        TokenPair pair = createTokens();
        Instant now = clock.instant();
        sessions.create(user.id(), hash(pair.accessToken()), hash(pair.refreshToken()), now,
            now.plus(accessLifetime), now.plus(refreshLifetime));
        users.updateLastAccess(user.id());
        return new LoginResult(pair.accessToken(), pair.refreshToken(), accessLifetime.toSeconds(), toPrincipal(0, user));
    }

    public TokenResult refresh(String refreshToken) {
        SessionRecord session = requireRefreshSession(refreshToken);
        AuthUser user = users.findById(session.userId()).orElseThrow(AuthService::invalidSession);
        validateActiveIdentity(user);
        TokenPair pair = createTokens();
        Instant now = clock.instant();
        boolean rotated = sessions.rotate(session.id(), session.refreshTokenHash(), hash(pair.accessToken()),
            hash(pair.refreshToken()), now.plus(accessLifetime), now.plus(refreshLifetime));
        if (!rotated) throw invalidSession();
        return new TokenResult(pair.accessToken(), pair.refreshToken(), accessLifetime.toSeconds());
    }

    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) return;
        sessions.findByRefreshHash(hash(refreshToken)).ifPresent(session -> sessions.revoke(session.id(), clock.instant()));
    }

    public LoginResult changePassword(String accessToken, String currentPassword, String newPassword) {
        AuthPrincipal principal = authenticate(accessToken);
        AuthUser user = users.findById(principal.userId()).orElseThrow(AuthService::invalidSession);
        if (!passwords.verify(currentPassword, user.passwordHash())) throw invalidCredentials();
        if (newPassword == null || newPassword.length() < 12)
            throw new AuthException(400, "WEAK_PASSWORD", "La nueva contraseña debe tener al menos 12 caracteres.");
        if (passwords.verify(newPassword, user.passwordHash()))
            throw new AuthException(400, "PASSWORD_REUSED", "La nueva contraseña debe ser diferente.");
        users.updatePassword(user.id(), passwords.hash(newPassword), false);
        Instant now = clock.instant(); sessions.revokeAllForUser(user.id(), now);
        AuthUser updated = users.findById(user.id()).orElseThrow(AuthService::invalidSession);
        TokenPair pair = createTokens();
        sessions.create(user.id(), hash(pair.accessToken()), hash(pair.refreshToken()), now, now.plus(accessLifetime), now.plus(refreshLifetime));
        return new LoginResult(pair.accessToken(), pair.refreshToken(), accessLifetime.toSeconds(), toPrincipal(0, updated));
    }

    public AuthPrincipal authenticate(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) throw invalidSession();
        SessionRecord session = sessions.findByAccessHash(hash(accessToken)).orElseThrow(AuthService::invalidSession);
        if (session.revoked() || !session.accessExpiresAt().isAfter(clock.instant())) throw invalidSession();
        AuthUser user = users.findById(session.userId()).orElseThrow(AuthService::invalidSession);
        validateActiveIdentity(user);
        return toPrincipal(session.id(), user);
    }

    private SessionRecord requireRefreshSession(String token) {
        if (token == null || token.isBlank()) throw invalidSession();
        SessionRecord session = sessions.findByRefreshHash(hash(token)).orElseThrow(AuthService::invalidSession);
        if (session.revoked() || !session.refreshExpiresAt().isAfter(clock.instant())) throw invalidSession();
        return session;
    }

    private void validateActiveIdentity(AuthUser user) {
        if ("BLOQUEADO".equals(user.userStatus())) throw new AuthException(403, "USER_BLOCKED", "La cuenta está bloqueada.");
        if (!"ACTIVO".equals(user.userStatus())) throw new AuthException(403, "USER_INACTIVE", "La cuenta está inactiva.");
        if (!SYSTEM_ROLES.contains(user.role())) throw new AuthException(403, "ROLE_NOT_ALLOWED", "El rol no está autorizado.");
        if ("MIEMBRO".equals(user.role())) {
            if (user.memberId() == null) throw new AuthException(403, "MEMBER_REQUIRED", "La cuenta no está asociada a un miembro.");
            if (!"ACTIVO".equals(user.memberStatus())) throw new AuthException(403, "MEMBER_INACTIVE", "El miembro está inactivo.");
        }
    }

    private AuthPrincipal toPrincipal(long sessionId, AuthUser user) {
        return new AuthPrincipal(sessionId, user.id(), user.memberId(), user.username(), user.role(), user.passwordChangeRequired(),
            user.memberNames(), user.memberLastNames(), user.memberStatus());
    }

    private TokenPair createTokens() { return new TokenPair(tokens.generate(), tokens.generate()); }
    private static String hash(String token) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8))); }
        catch (Exception exception) { throw new IllegalStateException("SHA-256 no está disponible.", exception); }
    }
    private static AuthException invalidCredentials() { return new AuthException(401, "INVALID_CREDENTIALS", "Usuario o contraseña incorrectos."); }
    private static AuthException invalidSession() { return new AuthException(401, "INVALID_TOKEN", "La sesión no es válida o expiró."); }
    private static long readPositive(String name, long fallback) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) return fallback;
        try { long parsed = Long.parseLong(value); return parsed > 0 ? parsed : fallback; }
        catch (NumberFormatException ignored) { return fallback; }
    }

    private record TokenPair(String accessToken, String refreshToken) { }
    public record LoginResult(String accessToken, String refreshToken, long expiresIn, AuthPrincipal principal) { }
    public record TokenResult(String accessToken, String refreshToken, long expiresIn) { }
}
