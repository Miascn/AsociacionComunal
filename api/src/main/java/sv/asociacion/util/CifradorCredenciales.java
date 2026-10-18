package sv.asociacion.util;

import sv.asociacion.config.AppConfig;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Cifra en reposo la contrasena provisional ({@code usuario.clave_temporal}).
 *
 * <h2>Por que cifrado reversible y no hash</h2>
 * <p>La aplicacion de escritorio vuelve a mostrar la contrasena provisional (y su
 * codigo QR de acceso) mientras el miembro no la cambie: es la forma en que la
 * directiva entrega el acceso. Un hash haria imposible esa funcion. Lo que si se
 * evita es que la clave quede legible en la base de datos, en un respaldo o en un
 * volcado: se guarda cifrada con AES-256-GCM y solo la API, que tiene la llave,
 * puede recuperarla. Cuando el miembro cambia su contrasena la columna se borra.</p>
 *
 * <h2>Llave</h2>
 * <p>Se toma de {@code CREDENTIALS_KEY} si existe; si no, se deriva de
 * {@code JWT_SECRET} (obligatorio para iniciar la API) con SHA-256 y un prefijo de
 * dominio, para no reutilizar la misma llave en dos usos. Nunca se escribe en el
 * codigo ni en la base.</p>
 *
 * <h2>Compatibilidad</h2>
 * <p>Los valores cifrados llevan el prefijo {@value #PREFIJO}. Un valor sin prefijo
 * es una clave antigua guardada en texto plano y se devuelve tal cual; se cifra la
 * proxima vez que se genere. Si un valor cifrado no puede descifrarse (llave
 * cambiada o dato alterado), se devuelve {@code null}: el servicio genera entonces
 * una contrasena provisional nueva en lugar de fallar.</p>
 */
public final class CifradorCredenciales {

    static final String PREFIJO = "enc:v1:";
    private static final String DOMINIO = "asociacion-comunal:clave_temporal:v1:";
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;
    private static final SecureRandom RANDOM = new SecureRandom();

    private static volatile CifradorCredenciales porDefecto;

    private final SecretKeySpec llave;

    /** @param secreto material del que se deriva la llave AES de 256 bits */
    public CifradorCredenciales(String secreto) {
        if (secreto == null || secreto.length() < 32) {
            throw new IllegalArgumentException("El secreto de cifrado debe tener al menos 32 caracteres.");
        }
        this.llave = new SecretKeySpec(sha256(DOMINIO + secreto), "AES");
    }

    /** Instancia de la aplicacion, con la llave de CREDENTIALS_KEY o JWT_SECRET. */
    public static CifradorCredenciales porDefecto() {
        CifradorCredenciales actual = porDefecto;
        if (actual == null) {
            synchronized (CifradorCredenciales.class) {
                actual = porDefecto;
                if (actual == null) {
                    String secreto = Objects.requireNonNullElse(AppConfig.setting("CREDENTIALS_KEY"), "");
                    if (secreto.isBlank()) secreto = Objects.requireNonNullElse(AppConfig.setting("JWT_SECRET"), "");
                    if (secreto.isBlank()) {
                        throw new IllegalStateException("Falta CREDENTIALS_KEY o JWT_SECRET para proteger las claves provisionales.");
                    }
                    actual = new CifradorCredenciales(secreto);
                    porDefecto = actual;
                }
            }
        }
        return actual;
    }

    public String cifrar(String textoPlano) {
        if (textoPlano == null) return null;
        try {
            byte[] iv = new byte[IV_BYTES];
            RANDOM.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, llave, new GCMParameterSpec(TAG_BITS, iv));
            byte[] cifrado = cipher.doFinal(textoPlano.getBytes(StandardCharsets.UTF_8));
            byte[] salida = Arrays.copyOf(iv, IV_BYTES + cifrado.length);
            System.arraycopy(cifrado, 0, salida, IV_BYTES, cifrado.length);
            return PREFIJO + Base64.getEncoder().encodeToString(salida);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No fue posible cifrar la clave provisional.", e);
        }
    }

    /**
     * @return la clave en claro; el mismo valor si es un dato antiguo sin cifrar;
     *         {@code null} si el dato cifrado no se puede recuperar
     */
    public String descifrar(String almacenado) {
        if (almacenado == null) return null;
        if (!almacenado.startsWith(PREFIJO)) return almacenado;
        try {
            byte[] datos = Base64.getDecoder().decode(almacenado.substring(PREFIJO.length()));
            if (datos.length <= IV_BYTES) return null;
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, llave, new GCMParameterSpec(TAG_BITS, datos, 0, IV_BYTES));
            byte[] plano = cipher.doFinal(datos, IV_BYTES, datos.length - IV_BYTES);
            return new String(plano, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            return null;
        }
    }

    private static byte[] sha256(String valor) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(valor.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("SHA-256 no está disponible.", e);
        }
    }
}
