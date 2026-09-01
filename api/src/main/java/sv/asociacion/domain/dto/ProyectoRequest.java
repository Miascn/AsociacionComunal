package sv.asociacion.domain.dto;

import java.math.BigDecimal;
import sv.asociacion.domain.entity.Proyecto;

public record ProyectoRequest(
    String nombre,
    String descripcion,
    BigDecimal presupuesto,
    Integer creadoPor,
    Proyecto.Estado estado
) {}
