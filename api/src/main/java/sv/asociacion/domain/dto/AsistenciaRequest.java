package sv.asociacion.domain.dto;

public record AsistenciaRequest(
    Integer idMiembro,
    boolean asistio,
    String observacion
) {}
