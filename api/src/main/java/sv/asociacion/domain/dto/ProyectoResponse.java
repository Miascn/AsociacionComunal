package sv.asociacion.domain.dto;

import sv.asociacion.domain.entity.Proyecto;
import java.math.BigDecimal;

public record ProyectoResponse(
    Integer id,
    String nombre,
    String descripcion,
    BigDecimal presupuesto,
    String fechaCreacion,
    String estado
) {
    public static ProyectoResponse from(Proyecto proyecto) {
        return new ProyectoResponse(
            proyecto.getIdProyecto(),
            proyecto.getNombre(),
            proyecto.getDescripcion(),
            proyecto.getPresupuesto(),
            proyecto.getFechaCreacion() == null ? null : proyecto.getFechaCreacion().toString(),
            proyecto.getEstado() == null ? null : proyecto.getEstado().name()
        );
    }
}