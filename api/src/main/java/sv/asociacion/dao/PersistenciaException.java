package sv.asociacion.dao;

/**
 * Fallo de persistencia que no puede presentarse al cliente como un exito.
 *
 * <p>Se usa donde tragar la {@link java.sql.SQLException} produciria una respuesta
 * enganosa: es preferible un error explicito a un registro fantasma.
 */
public class PersistenciaException extends RuntimeException {

    public PersistenciaException(String message, Throwable cause) {
        super(message, cause);
    }
}
