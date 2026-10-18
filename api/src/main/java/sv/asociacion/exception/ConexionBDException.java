package sv.asociacion.exception;

/**
 * La base de datos no esta disponible: no se pudo obtener una conexion del pool
 * (servidor caido, credenciales invalidas, red) o el pool esta agotado y venció
 * {@code maxWait}.
 *
 * <p>Se distingue de un {@link PersistenciaException} generico porque el remedio es
 * otro: no es un error de la solicitud ni un defecto del codigo, es un servicio
 * externo temporalmente fuera de alcance. Se traduce a HTTP 503.</p>
 */
public class ConexionBDException extends PersistenciaException {

    public ConexionBDException(String message, Throwable cause) {
        super(message, cause);
    }
}
