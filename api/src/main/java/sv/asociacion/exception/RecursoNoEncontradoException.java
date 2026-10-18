package sv.asociacion.exception;

/** El recurso solicitado (miembro, reunion, votacion...) no existe. HTTP 404. */
public class RecursoNoEncontradoException extends NegocioException {

    public RecursoNoEncontradoException(String message) {
        super(message);
    }
}
