package service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class QaApiConfigTest {

    private static final String TOKEN = "0123456789abcdef0123456789abcdef";

    @Test
    @DisplayName("HTTPS se acepta y se normaliza sin barra final")
    void aceptaHttps() {
        assertEquals("https://api.ejemplo.org", QaApiConfig.validated("https://api.ejemplo.org/", TOKEN).baseUrl());
    }

    @Test
    @DisplayName("HTTP se acepta solo hacia localhost o 127.0.0.1 (API local de evaluacion)")
    void aceptaHttpLocal() {
        assertEquals("http://localhost:8080", QaApiConfig.validated("http://localhost:8080", TOKEN).baseUrl());
        assertEquals("http://127.0.0.1:8080", QaApiConfig.validated("http://127.0.0.1:8080/", TOKEN).baseUrl());
    }

    @Test
    @DisplayName("HTTP hacia cualquier otro host sigue rechazado")
    void rechazaHttpRemoto() {
        assertThrows(IllegalStateException.class, () -> QaApiConfig.validated("http://192.168.1.10:8080", TOKEN));
        assertThrows(IllegalStateException.class, () -> QaApiConfig.validated("http://localhost.evil.com", TOKEN));
        assertThrows(IllegalStateException.class, () -> QaApiConfig.validated("http://127.0.0.1.nip.io", TOKEN));
    }

    @Test
    @DisplayName("El token debe tener al menos 32 caracteres")
    void tokenCorto() {
        assertThrows(IllegalStateException.class, () -> QaApiConfig.validated("http://localhost:8080", "corto"));
    }
}
