package sv.asociacion.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordHasherTest {

    @Test
    @DisplayName("La contrasena correcta se verifica contra su hash")
    void verificaContrasenaCorrecta() {
        String hash = PasswordHasher.hash("Segura123!");
        assertTrue(PasswordHasher.verify("Segura123!", hash));
    }

    @Test
    @DisplayName("Una contrasena distinta no verifica")
    void rechazaContrasenaIncorrecta() {
        String hash = PasswordHasher.hash("Segura123!");
        assertFalse(PasswordHasher.verify("segura123!", hash));
    }

    @Test
    @DisplayName("El hash no contiene la contrasena y usa sal aleatoria: dos hashes del mismo valor difieren")
    void salAleatoria() {
        String a = PasswordHasher.hash("Segura123!");
        String b = PasswordHasher.hash("Segura123!");
        assertNotEquals(a, b);
        assertFalse(a.contains("Segura123!"));
        assertTrue(a.matches("[0-9a-f]{32}:[0-9a-f]{64}"), "sal de 16 bytes y clave PBKDF2 de 256 bits en hexadecimal");
    }

    @Test
    @DisplayName("Entradas nulas o hashes corruptos devuelven false en lugar de lanzar")
    void entradasInvalidas() {
        assertFalse(PasswordHasher.verify(null, "abc"));
        assertFalse(PasswordHasher.verify("x", null));
        assertFalse(PasswordHasher.verify("x", "sin-separador"));
        assertFalse(PasswordHasher.verify("x", "zz:no-es-hex"));
        assertFalse(PasswordHasher.verify("x", "pbkdf2_sha256$no-numero$a$b"));
    }
}
