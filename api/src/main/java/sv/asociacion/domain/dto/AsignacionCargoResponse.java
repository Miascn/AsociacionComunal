package sv.asociacion.domain.dto;

public record AsignacionCargoResponse(
    Integer id,
    Integer idMiembro,
    String nombreMiembro,
    String duiMiembro,
    String telefonoMiembro,
    Integer idCargo,
    String nombreCargo,
    Integer nivelJerarquico,
    Integer idPeriodo,
    String nombrePeriodo,
    String estadoPeriodo,
    String fechaAsignacion,
    String fechaFin,
    String motivoSalida,
    String estado
) {}
