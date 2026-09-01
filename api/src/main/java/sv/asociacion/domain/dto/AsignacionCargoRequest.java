package sv.asociacion.domain.dto;

import java.time.LocalDate;

public record AsignacionCargoRequest(
    Integer idMiembro,
    Integer idCargo,
    Integer idPeriodo,
    LocalDate fechaAsignacion,
    LocalDate fechaFin
) {}
