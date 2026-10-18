package sv.asociacion.exception;

/**
 * Fallo al leer o escribir en la base de datos.
 *
 * <p>Envuelve la {@link java.sql.SQLException} original como causa: el detalle
 * tecnico (SQLState, codigo de MySQL, texto del servidor) queda disponible para el
 * registro del servidor, pero el mensaje de esta excepcion es un texto seguro que
 * puede mostrarse al cliente sin revelar SQL ni estructura de tablas.</p>
 *
 * <p>Existe para que un fallo de persistencia nunca se presente como exito: antes,
 * la mayoria de los DAO capturaban la SQLException, la imprimian y devolvian una
 * lista vacia o {@code false}, de modo que una caida de la base se veia como
 * "no hay datos". Se traduce a HTTP 500.</p>
 */
public class PersistenciaException extends RuntimeException {

    public PersistenciaException(String message, Throwable cause) {
        super(message, cause);
    }
}
