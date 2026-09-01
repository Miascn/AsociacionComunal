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
    public record OpcionDetalle(
        Integer idOpcion,
        String descripcion,
        int orden,
        int votos
    ) {}
}
