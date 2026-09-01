package sv.asociacion.domain.dto;

public record EstadoParticipacionResponse(
    Integer idVotacion,
    Integer idMiembro,
    boolean yaVoto,
    String fechaHoraVoto
) {}
