package sv.asociacion.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Resultado agregado de una convocatoria concurrente.
 *
 * @param idReunion          reunion convocada
 * @param solicitados        miembros que se intento convocar
 * @param convocados         registros de asistencia creados en esta ejecucion
 * @param omitidos           ya estaban convocados o no estan activos (no se duplicaron)
 * @param lotes              tareas en que se dividio el trabajo
 * @param lotesFallidos      tareas que terminaron con error
 * @param hilos              hilos del ExecutorService usados
 * @param duracionMs         duracion total
 * @param miembrosPendientes miembros de los lotes fallidos; repetir la convocatoria los incluye
 */
public record ResultadoConvocatoria(
    int idReunion,
    int solicitados,
    int convocados,
    int omitidos,
    int lotes,
    int lotesFallidos,
    int hilos,
    long duracionMs,
    List<Integer> miembrosPendientes
) {
    public ResultadoConvocatoria {
        miembrosPendientes = List.copyOf(miembrosPendientes);
    }

    @JsonProperty("completa")
    public boolean completa() {
        return lotesFallidos == 0;
    }
}
