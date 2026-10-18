package sv.asociacion.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class GeneradorClaveTemporalTest {

    @Test
    @DisplayName("La clave tiene el prefijo y la longitud acordados")
    void formato() {
        String clave = GeneradorClaveTemporal.generar();
        assertTrue(clave.startsWith("Tmp!"));
        assertEquals(4 + GeneradorClaveTemporal.LONGITUD_ALEATORIA, clave.length());
    }

    @Test
    @DisplayName("La parte aleatoria solo usa el alfabeto sin caracteres ambiguos (0, O, 1, l, I)")
    void alfabetoSinAmbiguos() {
        for (int i = 0; i < 200; i++) {
            String aleatoria = GeneradorClaveTemporal.generar().substring(4);
            for (char c : aleatoria.toCharArray()) {
                assertTrue(GeneradorClaveTemporal.ALFABETO.indexOf(c) >= 0, "caracter fuera del alfabeto: " + c);
                assertFalse("0O1lI".indexOf(c) >= 0, "caracter ambiguo: " + c);
            }
        }
    }

    @Test
    @DisplayName("Dos claves generadas no se repiten en la practica")
    void noSeRepiten() {
        Set<String> vistas = new HashSet<>();
        for (int i = 0; i < 1_000; i++) {
            assertTrue(vistas.add(GeneradorClaveTemporal.generar()), "clave repetida en la iteracion " + i);
        }
    }
}
