package security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import models.AuthUser;

class SessionManagerTest {
    private final SessionManager sessionManager = SessionManager.getInstance();

    @AfterEach
    void limpiarSesion() {
        sessionManager.clear();
    }

    @Test
    void iniciaYFinalizaLaSesion() {
        AuthUser user = new AuthUser("admin", "Administrador", "Administrador", "hash", "salt");

        sessionManager.start(user);

        assertTrue(sessionManager.isAuthenticated());
        assertEquals(user, sessionManager.requireCurrentUser());

        sessionManager.clear();

        assertFalse(sessionManager.isAuthenticated());
    }
}
