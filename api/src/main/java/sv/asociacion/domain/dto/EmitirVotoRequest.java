package sv.asociacion.domain.dto;

public record EmitirVotoRequest(
    Integer idVotacion,
    Integer idOpcion,
    Integer idMiembro
) {}
