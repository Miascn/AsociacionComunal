package sv.asociacion.domain.dto;

public record AsistenciaResponse(
    Long idAsistencia,
    Integer idReunion,
    Integer idMiembro,
    String nombreMiembro,
    String duiMiembro,
    String telefonoMiembro,
    boolean asistio,
    String observacion
) {}
