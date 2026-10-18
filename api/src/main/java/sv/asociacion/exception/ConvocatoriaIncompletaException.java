package sv.asociacion.exception;

import sv.asociacion.domain.dto.ResultadoConvocatoria;

/**
 * Uno o mas lotes de la convocatoria concurrente fallaron al escribir en la base.
 *
 * <p>Conserva el resultado parcial: cuantos miembros si quedaron convocados y
 * cuales quedaron pendientes. Como la operacion es idempotente, repetirla completa
 * a los pendientes sin duplicar a los que ya estan.</p>
 */
public class ConvocatoriaIncompletaException extends PersistenciaException {

    private final transient ResultadoConvocatoria resultado;

    public ConvocatoriaIncompletaException(ResultadoConvocatoria resultado, Throwable primeraCausa) {
        super("Se convocaron " + resultado.convocados() + " miembros, pero " + resultado.miembrosPendientes().size()
            + " quedaron pendientes por un error de base de datos. La convocatoria puede repetirse sin duplicar registros.",
            primeraCausa);
        this.resultado = resultado;
    }

    public ResultadoConvocatoria resultado() {
        return resultado;
    }

    /** True si la causa fue que la base de datos no estaba disponible. */
    public boolean porBaseNoDisponible() {
        return getCause() instanceof ConexionBDException;
    }
}
