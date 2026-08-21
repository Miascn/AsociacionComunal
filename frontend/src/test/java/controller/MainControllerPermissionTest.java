package controller;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class MainControllerPermissionTest {
    @Test void onlyAdministratorsCanSeeUserManagement() {
        assertTrue(MainController.isAdministrator("ADMIN"));
        assertTrue(MainController.isAdministrator("Administrador"));
        assertFalse(MainController.isAdministrator("MIEMBRO"));
        assertFalse(MainController.isAdministrator(null));
    }
}
