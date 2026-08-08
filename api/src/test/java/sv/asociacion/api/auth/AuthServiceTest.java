package sv.asociacion.api.auth;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class AuthServiceTest {
    private static String VALID_HASH;
    private FakeUsers users;
    private FakeSessions sessions;
    private MutableClock clock;
    private AuthService service;

    @BeforeAll static void hashPassword() { VALID_HASH = new PasswordService().hash("Correcta2026!"); }

    @BeforeEach void setUp() {
        users = new FakeUsers(); sessions = new FakeSessions();
        clock = new MutableClock(Instant.parse("2026-08-08T00:00:00Z"));
        users.add(memberUser(1, "ACTIVO", 10, "ACTIVO", "MIEMBRO"));
        service = new AuthService(users, sessions, new PasswordService(), new SequentialTokens(), clock,
            Duration.ofMinutes(15), Duration.ofDays(30));
    }

    @Test void loginCorrecto() {
        var result = service.login("residente", "Correcta2026!");
        assertEquals("MIEMBRO", result.principal().role()); assertNotNull(result.accessToken()); assertNotNull(result.refreshToken());
    }
    @Test void usuarioInexistente() { assertCode("INVALID_CREDENTIALS", () -> service.login("nadie", "Correcta2026!")); }
    @Test void contrasenaIncorrecta() { assertCode("INVALID_CREDENTIALS", () -> service.login("residente", "incorrecta")); }
    @Test void usuarioBloqueado() { users.replace(memberUser(1, "BLOQUEADO", 10, "ACTIVO", "MIEMBRO")); assertCode("USER_BLOCKED", this::login); }
    @Test void usuarioInactivo() { users.replace(memberUser(1, "INACTIVO", 10, "ACTIVO", "MIEMBRO")); assertCode("USER_INACTIVE", this::login); }
    @Test void miembroInactivo() { users.replace(memberUser(1, "ACTIVO", 10, "INACTIVO", "MIEMBRO")); assertCode("MEMBER_INACTIVE", this::login); }
    @Test void miembroSinAsociacion() { users.replace(memberUser(1, "ACTIVO", null, null, "MIEMBRO")); assertCode("MEMBER_REQUIRED", this::login); }

    @Test void accessTokenValido() { var login = login(); assertEquals(1, service.authenticate(login.accessToken()).userId()); }
    @Test void accessTokenInvalido() { assertCode("INVALID_TOKEN", () -> service.authenticate("invalido")); }
    @Test void accessTokenVencido() { var login = login(); clock.advance(Duration.ofMinutes(16)); assertCode("INVALID_TOKEN", () -> service.authenticate(login.accessToken())); }

    @Test void refreshTokenValidoYRotado() {
        var login = login(); var refreshed = service.refresh(login.refreshToken());
        assertNotEquals(login.accessToken(), refreshed.accessToken());
        assertCode("INVALID_TOKEN", () -> service.refresh(login.refreshToken()));
        assertEquals(1, service.authenticate(refreshed.accessToken()).userId());
    }
    @Test void refreshTokenInvalido() { assertCode("INVALID_TOKEN", () -> service.refresh("invalido")); }
    @Test void refreshTokenRevocado() { var login = login(); service.logout(login.refreshToken()); assertCode("INVALID_TOKEN", () -> service.refresh(login.refreshToken())); }
    @Test void logoutRevocaAccessYRefresh() {
        var login = login(); service.logout(login.refreshToken());
        assertCode("INVALID_TOKEN", () -> service.authenticate(login.accessToken()));
        assertCode("INVALID_TOKEN", () -> service.refresh(login.refreshToken()));
    }
    @Test void cambioClaveReemplazaTemporalYRevocaSesionAnterior() {
        AuthUser original = memberUser(1, "ACTIVO", 10, "ACTIVO", "MIEMBRO");
        users.replace(new AuthUser(original.id(), original.memberId(), original.username(), original.passwordHash(), original.userStatus(), true, original.role(), original.memberNames(), original.memberLastNames(), original.memberStatus()));
        var login = login();
        assertTrue(login.principal().passwordChangeRequired());
        var changed = service.changePassword(login.accessToken(), "Correcta2026!", "NuevaCorrecta2026!");
        assertFalse(changed.principal().passwordChangeRequired());
        assertCode("INVALID_TOKEN", () -> service.authenticate(login.accessToken()));
        assertCode("INVALID_CREDENTIALS", this::login);
        assertEquals(1, service.login("residente", "NuevaCorrecta2026!").principal().userId());
    }
    @Test void rolIncorrectoEsRechazado() {
        AuthPrincipal principal = new AuthPrincipal(1, 1, null, "tesorero", "TESORERO", false, null, null, null);
        AuthException error = assertThrows(AuthException.class,
            () -> new AuthorizationService().requireAnyRole(principal, Set.of("MIEMBRO")));
        assertEquals("FORBIDDEN", error.code());
    }

    private AuthService.LoginResult login() { return service.login("residente", "Correcta2026!"); }
    private static void assertCode(String code, Runnable action) { assertEquals(code, assertThrows(AuthException.class, action::run).code()); }
    private static AuthUser memberUser(int id, String userStatus, Integer memberId, String memberStatus, String role) {
        return new AuthUser(id, memberId, "residente", VALID_HASH, userStatus, false, role, "Juan", "Pérez", memberStatus);
    }

    private static final class SequentialTokens implements TokenGenerator {
        private int value; public String generate() { return "token-seguro-de-prueba-" + (++value); }
    }
    private static final class MutableClock extends Clock {
        private Instant instant; MutableClock(Instant instant) { this.instant = instant; }
        void advance(Duration duration) { instant = instant.plus(duration); }
        public ZoneId getZone() { return ZoneOffset.UTC; }
        public Clock withZone(ZoneId zone) { return this; }
        public Instant instant() { return instant; }
    }
    private static final class FakeUsers implements UserAuthRepository {
        private final Map<Integer, AuthUser> values = new HashMap<>();
        void add(AuthUser user) { values.put(user.id(), user); } void replace(AuthUser user) { values.put(user.id(), user); }
        public Optional<AuthUser> findByUsername(String username) { return values.values().stream().filter(u -> u.username().equals(username)).findFirst(); }
        public Optional<AuthUser> findById(int id) { return Optional.ofNullable(values.get(id)); }
        public void updateLastAccess(int id) { }
        public void updatePassword(int id, String hash, boolean required) { AuthUser u=values.get(id); values.put(id, new AuthUser(u.id(),u.memberId(),u.username(),hash,u.userStatus(),required,u.role(),u.memberNames(),u.memberLastNames(),u.memberStatus())); }
    }
    private static final class FakeSessions implements SessionRepository {
        private long next = 1; private final Map<Long, SessionRecord> values = new HashMap<>();
        public SessionRecord create(int userId, String access, String refresh, Instant created, Instant accessExp, Instant refreshExp) {
            var value = new SessionRecord(next++, userId, access, refresh, created, accessExp, refreshExp, null); values.put(value.id(), value); return value;
        }
        public Optional<SessionRecord> findByAccessHash(String hash) { return values.values().stream().filter(s -> s.accessTokenHash().equals(hash)).findFirst(); }
        public Optional<SessionRecord> findByRefreshHash(String hash) { return values.values().stream().filter(s -> s.refreshTokenHash().equals(hash)).findFirst(); }
        public boolean rotate(long id, String expected, String access, String refresh, Instant accessExp, Instant refreshExp) {
            SessionRecord old = values.get(id); if (old == null || old.revoked() || !old.refreshTokenHash().equals(expected)) return false;
            values.put(id, new SessionRecord(id, old.userId(), access, refresh, old.createdAt(), accessExp, refreshExp, old.revokedAt())); return true;
        }
        public void revoke(long id, Instant at) { SessionRecord old = values.get(id); values.put(id, new SessionRecord(id, old.userId(), old.accessTokenHash(), old.refreshTokenHash(), old.createdAt(), old.accessExpiresAt(), old.refreshExpiresAt(), at)); }
        public void revokeAllForUser(int userId, Instant at) { values.values().stream().filter(s -> s.userId()==userId && !s.revoked()).map(SessionRecord::id).toList().forEach(id -> revoke(id, at)); }
    }
}
