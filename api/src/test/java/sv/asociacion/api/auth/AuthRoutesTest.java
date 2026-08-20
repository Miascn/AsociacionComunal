package sv.asociacion.api.auth;

import io.javalin.Javalin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class AuthRoutesTest {
    private Javalin app;
    private HttpClient client;
    private AuthService service;
    private String accessToken;

    @BeforeEach void startServer() {
        PasswordService passwords = new PasswordService();
        UserAuthRepository users = new MemoryUsers(List.of(
            new AuthUser(1, 10, "residente", passwords.hash("Correcta2026!"), "ACTIVO", false, "MIEMBRO", "Juan", "Pérez", "ACTIVO"),
            new AuthUser(2, null, "tesorero", passwords.hash("Correcta2026!"), "ACTIVO", false, "TESORERO", null, null, null)
        ));
        service = new AuthService(users, new MemorySessions(), passwords, new RandomTokens(), Clock.systemUTC(), Duration.ofMinutes(15), Duration.ofDays(30));
        AuthRoutes routes = new AuthRoutes(service);
        app = Javalin.create(config -> {
            routes.register(config.routes);
            config.routes.get("/api/member-only", context -> {
                AuthPrincipal principal = routes.requirePrincipal(context);
                new AuthorizationService().requireAnyRole(principal, Set.of("MIEMBRO"));
                context.status(204);
            });
        }).start("127.0.0.1", 0);
        client = HttpClient.newHttpClient();
        accessToken = service.login("residente", "Correcta2026!").accessToken();
    }

    @AfterEach void stopServer() { if (app != null) app.stop(); }

    @Test void apiMeSinToken() throws Exception { assertEquals(401, get("/api/me", null).statusCode()); }

    @Test void apiMeConTokenValido() throws Exception {
        HttpResponse<String> response = get("/api/me", accessToken);
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("\"username\":\"residente\""));
        assertTrue(response.body().contains("\"names\":\"Juan\""));
        assertFalse(response.body().contains("clave_hash"));
        assertFalse(response.body().contains("passwordHash"));
    }

    @Test void endpointProtegidoRechazaRolIncorrecto() throws Exception {
        String treasurerToken = service.login("tesorero", "Correcta2026!").accessToken();
        assertEquals(403, get("/api/member-only", treasurerToken).statusCode());
    }

    private HttpResponse<String> get(String path, String token) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + app.port() + path)).GET();
        if (token != null) request.header("Authorization", "Bearer " + token);
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static final class RandomTokens implements TokenGenerator {
        public String generate() { return UUID.randomUUID().toString(); }
    }
    private static final class MemoryUsers implements UserAuthRepository {
        private final List<AuthUser> users; MemoryUsers(List<AuthUser> users) { this.users = users; }
        public Optional<AuthUser> findByUsername(String username) { return users.stream().filter(u -> u.username().equals(username)).findFirst(); }
        public Optional<AuthUser> findById(int id) { return users.stream().filter(u -> u.id() == id).findFirst(); }
        public void updateLastAccess(int id) { }
        public void updatePassword(int id, String hash, boolean required) { }
    }
    private static final class MemorySessions implements SessionRepository {
        private long next = 1; private final Map<Long, SessionRecord> sessions = new HashMap<>();
        public SessionRecord create(int userId, String access, String refresh, Instant created, Instant accessExp, Instant refreshExp) {
            SessionRecord value = new SessionRecord(next++, userId, access, refresh, created, accessExp, refreshExp, null); sessions.put(value.id(), value); return value;
        }
        public Optional<SessionRecord> findByAccessHash(String hash) { return sessions.values().stream().filter(s -> s.accessTokenHash().equals(hash)).findFirst(); }
        public Optional<SessionRecord> findByRefreshHash(String hash) { return sessions.values().stream().filter(s -> s.refreshTokenHash().equals(hash)).findFirst(); }
        public boolean rotate(long id, String expected, String access, String refresh, Instant accessExp, Instant refreshExp) {
            SessionRecord old = sessions.get(id); if (old == null || old.revoked() || !old.refreshTokenHash().equals(expected)) return false;
            sessions.put(id, new SessionRecord(id, old.userId(), access, refresh, old.createdAt(), accessExp, refreshExp, old.revokedAt())); return true;
        }
        public void revoke(long id, Instant at) { SessionRecord old = sessions.get(id); sessions.put(id, new SessionRecord(id, old.userId(), old.accessTokenHash(), old.refreshTokenHash(), old.createdAt(), old.accessExpiresAt(), old.refreshExpiresAt(), at)); }
        public void revokeAllForUser(int userId, Instant at) { sessions.values().stream().filter(s -> s.userId()==userId && !s.revoked()).map(SessionRecord::id).toList().forEach(id -> revoke(id, at)); }
    }
}
