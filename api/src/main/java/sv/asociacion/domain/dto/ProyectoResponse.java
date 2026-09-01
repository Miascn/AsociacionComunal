package sv.asociacion.domain.dto;

import java.math.BigDecimal;
import sv.asociacion.domain.entity.Proyecto;

public record ProyectoResponse(
    Integer id,
    Integer creadoPor,
    String nombreCreador,
    String nombre,
    String descripcion,
    BigDecimal presupuesto,
    String fechaCreacion,
    String estado
) {
    public static ProyectoResponse from(Proyecto proyecto) {
        return from(proyecto, null);
    }

    public static ProyectoResponse from(Proyecto proyecto, String nombreCreador) {
        return new ProyectoResponse(
            proyecto.getIdProyecto(),
            proyecto.getCreadoPor(),
            nombreCreador != null ? nombreCreador : (proyecto.getCreadoPor() != null ? "Usuario #" + proyecto.getCreadoPor() : "Sistema"),
            proyecto.getNombre(),
            proyecto.getDescripcion(),
            proyecto.getPresupuesto() != null ? proyecto.getPresupuesto() : BigDecimal.ZERO,
            proyecto.getFechaCreacion() == null ? null : proyecto.getFechaCreacion().toString(),
            proyecto.getEstado() == null ? null : proyecto.getEstado().name()
        );
    }
}