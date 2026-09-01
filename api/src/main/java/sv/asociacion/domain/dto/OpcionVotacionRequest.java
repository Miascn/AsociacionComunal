package sv.asociacion.domain.dto;

public record OpcionVotacionRequest(
    Integer idVotacion,
    String descripcion,
    Short orden
) {}
