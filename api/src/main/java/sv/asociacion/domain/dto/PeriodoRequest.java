package sv.asociacion.domain.dto;

import java.time.LocalDate;
import sv.asociacion.domain.entity.PeriodoDirectiva;

public record PeriodoRequest(
    String nombre,
    LocalDate fechaInicio,
    LocalDate fechaFin,
    PeriodoDirectiva.Estado estado
) {}
