package service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class UpdateServiceVersionTest {

    @Test
    void comparacionSemanticaNumerica() {
        // Casos clave de 1.4.9 vs 1.4.10 vs 1.4.15
        assertTrue(UpdateService.compare("1.4.15", "1.4.14") > 0);
        assertTrue(UpdateService.compare("1.4.10", "1.4.9") > 0);
        assertTrue(UpdateService.compare("1.4.9", "1.4.10") < 0);
        assertEquals(0, UpdateService.compare("1.4.15", "1.4.15"));
        assertTrue(UpdateService.compare("1.5.0", "1.4.99") > 0);
        assertTrue(UpdateService.compare("2.0.0", "1.99.99") > 0);
    }

    @Test
    void toleranciaSegmentosYSufijos() {
        assertTrue(UpdateService.compare("1.4.15-SNAPSHOT", "1.4.14") > 0);
        assertTrue(UpdateService.compare("1.4.14", "0.0.0") > 0);
        assertEquals(0, UpdateService.compare("1.4", "1.4.0"));
        assertEquals(0, UpdateService.compare("1.4.0", "1.4"));
    }
}
