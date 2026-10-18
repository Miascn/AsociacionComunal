package sv.asociacion.exception;

/** Los datos enviados no cumplen una regla de formato u obligatoriedad. HTTP 400. */
public class ValidacionException extends NegocioException {

    public ValidacionException(String message) {
        super(message);
    }
}
