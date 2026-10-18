package sv.asociacion.util;

import java.security.SecureRandom;

/**
 * Genera las contrasenas provisionales que recibe un miembro o usuario al crear o
 * restablecer su cuenta.
 *
 * <p>Antes habia cuatro copias de este algoritmo (dos en MiembroService, una en
 * UsuarioService y otra en MemberProvisioningService) con prefijos y longitudes
 * distintos. Concentrarlo aqui garantiza un unico formato y una sola fuente de
 * aleatoriedad criptografica.</p>
 */
public final class GeneradorClaveTemporal {

    /** Sin caracteres ambiguos (0/O, 1/l/I) porque la clave se dicta o se copia a mano. */
    static final String ALFABETO = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$%";
    static final String PREFIJO = "Tmp!";
    static final int LONGITUD_ALEATORIA = 13;

    private static final SecureRandom RANDOM = new SecureRandom();

    private GeneradorClaveTemporal() {
    }

    public static String generar() {
        StringBuilder clave = new StringBuilder(PREFIJO);
        for (int i = 0; i < LONGITUD_ALEATORIA; i++) {
            clave.append(ALFABETO.charAt(RANDOM.nextInt(ALFABETO.length())));
        }
        return clave.toString();
    }
}
