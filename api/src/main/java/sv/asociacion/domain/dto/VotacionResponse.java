package sv.asociacion.domain.dto;

import java.util.List;

public record VotacionResponse(
    Integer id,
    String titulo,
    String descripcion,
    Integer idProyecto,
    String nombreProyecto,
    String fechaInicio,
    String fechaFin,
    String estado,
    int totalOpciones,
    int totalVotos,
    List<OpcionDetalle> opciones
) {
    /**
     * Resultado de una opcion. El {@code porcentaje} es la cuota de {@code votos} sobre
     * el total publicado de la votacion, redondeada a un decimal. Vale {@code 0.0}
     * mientras los resultados no esten publicados y cuando no hay votos, de modo que
     * nunca se divide entre cero.
     */
    public record OpcionDetalle(
        Integer idOpcion,
        String descripcion,
        int orden,
        int votos,
        double porcentaje
    ) {}
}
