package sv.asociacion.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class CifradorCredencialesTest {

    private static final String SECRETO = "secreto-de-prueba-de-al-menos-32-caracteres";
    private final CifradorCredenciales cifrador = new CifradorCredenciales(SECRETO);

    @Test
    @DisplayName("Lo que se cifra se recupera igual")
    void idaYVuelta() {
        String almacenado = cifrador.cifrar("Tmp!abcDEF23456#");
        assertEquals("Tmp!abcDEF23456#", cifrador.descifrar(almacenado));
    }

    @Test
    @DisplayName("El valor guardado no contiene la clave y lleva el prefijo de version")
    void noQuedaEnClaro() {
        String almacenado = cifrador.cifrar("Tmp!abcDEF23456#");
        assertTrue(almacenado.startsWith(CifradorCredenciales.PREFIJO));
        assertFalse(almacenado.contains("abcDEF"));
    }

    @Test
    @DisplayName("Cada cifrado usa un IV aleatorio: la misma clave produce valores distintos")
    void ivAleatorio() {
        assertNotEquals(cifrador.cifrar("misma"), cifrador.cifrar("misma"));
    }

    @Test
    @DisplayName("Un dato alterado no se acepta (autenticacion de GCM): se devuelve null")
    void datoAlterado() {
        String almacenado = cifrador.cifrar("Tmp!abcDEF23456#");
        byte[] bytes = Base64.getDecoder().decode(almacenado.substring(CifradorCredenciales.PREFIJO.length()));
        bytes[bytes.length - 1] ^= 0x01;
        String alterado = CifradorCredenciales.PREFIJO + Base64.getEncoder().encodeToString(bytes);
        assertNull(cifrador.descifrar(alterado));
    }

    @Test
    @DisplayName("Con otra llave no se puede descifrar: se devuelve null y el servicio genera una clave nueva")
    void otraLlave() {
        String almacenado = cifrador.cifrar("Tmp!abcDEF23456#");
        CifradorCredenciales otro = new CifradorCredenciales("otro-secreto-distinto-de-al-menos-32-chars");
        assertNull(otro.descifrar(almacenado));
    }

    @Test
    @DisplayName("Compatibilidad: una clave antigua guardada en texto plano se lee tal cual")
    void datoAntiguoEnClaro() {
        assertEquals("Tmp#legacy1234", cifrador.descifrar("Tmp#legacy1234"));
    }

    @Test
    @DisplayName("null se conserva como null en ambos sentidos; base64 invalido no lanza")
    void nulosYBasura() {
        assertNull(cifrador.cifrar(null));
        assertNull(cifrador.descifrar(null));
        assertNull(cifrador.descifrar(CifradorCredenciales.PREFIJO + "%%%no-es-base64"));
        assertNull(cifrador.descifrar(CifradorCredenciales.PREFIJO + "QUJD"));
    }

    @Test
    @DisplayName("Un secreto corto se rechaza")
    void secretoCorto() {
        assertThrows(IllegalArgumentException.class, () -> new CifradorCredenciales("corto"));
        assertThrows(IllegalArgumentException.class, () -> new CifradorCredenciales(null));
    }
}
