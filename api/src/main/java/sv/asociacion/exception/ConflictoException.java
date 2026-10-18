package sv.asociacion.exception;

/**
 * La solicitud es valida pero choca con el estado actual: duplicados, transiciones
 * de estado no permitidas, recursos en uso. HTTP 409.
 */
public class ConflictoException extends NegocioException {

    public ConflictoException(String message) {
        super(message);
    }

    public ConflictoException(String message, Throwable cause) {
        super(message, cause);
    }
}
