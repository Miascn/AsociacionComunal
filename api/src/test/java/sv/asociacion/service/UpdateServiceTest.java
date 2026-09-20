package sv.asociacion.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class UpdateServiceTest {

    @Test
    void comparacionSemanticaVersiones() {
        assertTrue(UpdateService.compareVersions("1.4.15", "1.4.14") > 0);
        assertTrue(UpdateService.compareVersions("1.4.10", "1.4.9") > 0);
        assertTrue(UpdateService.compareVersions("1.4.9", "1.4.10") < 0);
        assertEquals(0, UpdateService.compareVersions("1.4.15", "1.4.15"));
        assertTrue(UpdateService.compareVersions("1.5.0", "1.4.99") > 0);
        assertTrue(UpdateService.compareVersions("2.0.0", "1.99.99") > 0);
    }

    @Test
    void toleranciaFormatos() {
        assertTrue(UpdateService.compareVersions("1.4.15-SNAPSHOT", "1.4.14") > 0);
        assertEquals(0, UpdateService.compareVersions("1.4", "1.4.0"));
        assertEquals(0, UpdateService.compareVersions("1.4.0", "1.4"));
    }
}
