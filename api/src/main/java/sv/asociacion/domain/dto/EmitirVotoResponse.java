package sv.asociacion.domain.dto;

public record EmitirVotoResponse(
    Long idVoto,
    Integer idVotacion,
    String fechaHora,
    String mensaje
) {}
