package sv.asociacion.domain.dto;

public record OpcionVotacionResponse(
    Integer id,
    Integer idVotacion,
    String tituloVotacion,
    String estadoVotacion,
    String descripcion,
    short orden,
    int votos,
    boolean editable
) {}
