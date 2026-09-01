package sv.asociacion.domain.dto;

import java.time.LocalDate;
import sv.asociacion.domain.entity.PeriodoDirectiva;

public record PeriodoResponse(
    Integer id,
    String nombre,
    String fechaInicio,
    String fechaFin,
    String estado,
    boolean vigente
) {
    public static PeriodoResponse from(PeriodoDirectiva periodo) {
        LocalDate now = LocalDate.now();
        boolean vigente = periodo.getEstado() == PeriodoDirectiva.Estado.ACTIVO
            && (periodo.getFechaInicio() == null || !now.isBefore(periodo.getFechaInicio()))
            && (periodo.getFechaFin() == null || !now.isAfter(periodo.getFechaFin()));

        return new PeriodoResponse(
            periodo.getIdPeriodo(),
            periodo.getNombre(),
            periodo.getFechaInicio() != null ? periodo.getFechaInicio().toString() : null,
            periodo.getFechaFin() != null ? periodo.getFechaFin().toString() : null,
            periodo.getEstado() != null ? periodo.getEstado().name() : "PLANIFICADO",
            vigente
        );
    }
}
