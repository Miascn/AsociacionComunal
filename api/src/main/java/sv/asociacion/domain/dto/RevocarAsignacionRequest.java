package sv.asociacion.domain.dto;

import java.time.LocalDate;

public record RevocarAsignacionRequest(
    LocalDate fechaFin,
    String motivo
) {}
