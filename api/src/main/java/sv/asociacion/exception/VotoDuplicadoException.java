package sv.asociacion.exception;

/**
 * Se lanza cuando la restriccion {@code uk_voto_votacion_miembro} rechaza un segundo
 * voto del mismo miembro en la misma votacion.
 *
 * <p>Existe para que la violacion de unicidad llegue al servicio como un hecho de
 * negocio y no se pierda: antes se capturaba en el DAO y la API respondia 201 con el
 * identificador del voto en nulo. Es un {@link ConflictoException}: HTTP 409.</p>
 */
public class VotoDuplicadoException extends ConflictoException {

    public VotoDuplicadoException(String message, Throwable cause) {
        super(message, cause);
    }
}
