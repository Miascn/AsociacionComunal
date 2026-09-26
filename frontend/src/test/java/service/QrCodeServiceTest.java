package service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javafx.scene.image.Image;
import org.junit.jupiter.api.Test;

class QrCodeServiceTest {

    @Test
    void testBuildLoginUri() {
        String uri = QrCodeService.buildLoginUri("carlos.martinez", "Asoc2026#clave");
        assertEquals("asociacioncomunal://login?username=carlos.martinez&password=Asoc2026%23clave", uri);
    }

    @Test
    void testBuildHttpsLoginUri() {
        String uri = QrCodeService.buildHttpsLoginUri("ana.gomez", "Temporal123!");
        assertEquals("https://asociacioncomunal.sv/login?username=ana.gomez&password=Temporal123%21", uri);
    }

    @Test
    void testGenerateQr() {
        String uri = QrCodeService.buildLoginUri("demo.user", "demo123");
        Image qr = QrCodeService.generateQr(uri, 120, 120);

        assertNotNull(qr);
        assertEquals(120.0, qr.getWidth());
        assertEquals(120.0, qr.getHeight());
    }
}
