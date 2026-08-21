package controller;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MiembroPermissionTest {
    @Test
    void soloAdministradorPuedeGestionarMiembros() {
        assertTrue(MiembroController.puedeGestionar("ADMINISTRADOR"));
        assertTrue(MiembroController.puedeGestionar("ADMIN"));
        assertFalse(MiembroController.puedeGestionar("MIEMBRO"));
        assertFalse(MiembroController.puedeGestionar("TESORERO"));
        assertFalse(MiembroController.puedeGestionar(null));
    }
}
