package sv.asociacion.domain.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import sv.asociacion.domain.entity.Aportacion;

public record AportacionRequest(
    Integer idMiembro,
    Integer idProyecto,
    String periodoMes,
    BigDecimal monto,
    LocalDate fechaPago,
    Aportacion.MetodoPago metodoPago,
    String referencia
) {}
