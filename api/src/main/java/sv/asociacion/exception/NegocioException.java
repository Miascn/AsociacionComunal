package sv.asociacion.exception;

/**
 * Base de los errores de negocio: la solicitud viola una regla del dominio de la
 * asociacion (datos invalidos, recurso inexistente, estado incompatible).
 *
 * <p>Es abstracta porque nunca se lanza "un error de negocio" en general: siempre
 * es uno concreto, y cada subclase determina su codigo HTTP. Su mensaje se
 * redacta para el usuario final y se devuelve tal cual al cliente.</p>
 */
public abstract class NegocioException extends RuntimeException {

    protected NegocioException(String message) {
        super(message);
    }

    protected NegocioException(String message, Throwable cause) {
        super(message, cause);
    }
}
