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
    String estado,
    BigDecimal montoRecaudado,
    BigDecimal montoPendiente,
    Double progresoPorcentaje,
    Integer idVotacion,
    String tituloVotacion,
    Integer totalAportantes,
    BigDecimal aportePorMiembro
) {
    public ProyectoResponse(
        Integer id,
        Integer creadoPor,
        String nombreCreador,
        String nombre,
        String descripcion,
        BigDecimal presupuesto,
        String fechaCreacion,
        String estado
    ) {
        this(
            id,
            creadoPor,
            nombreCreador,
            nombre,
            descripcion,
            presupuesto,
            fechaCreacion,
            estado,
            BigDecimal.ZERO,
            presupuesto != null ? presupuesto : BigDecimal.ZERO,
            0.0,
            null,
            null,
            0,
            BigDecimal.ZERO
        );
    }

    public static ProyectoResponse from(Proyecto proyecto) {
        return from(proyecto, null);
    }

    public static ProyectoResponse from(Proyecto proyecto, String nombreCreador) {
        return from(proyecto, nombreCreador, BigDecimal.ZERO, null, null, 0, BigDecimal.ZERO);
    }

    public static ProyectoResponse from(
        Proyecto proyecto,
        String nombreCreador,
        BigDecimal montoRecaudado,
        Integer idVotacion,
        String tituloVotacion,
        Integer totalAportantes,
        BigDecimal aportePorMiembro
    ) {
        BigDecimal pres = proyecto.getPresupuesto() != null ? proyecto.getPresupuesto() : BigDecimal.ZERO;
        BigDecimal rec = montoRecaudado != null ? montoRecaudado : BigDecimal.ZERO;
        BigDecimal pend = pres.subtract(rec);
        if (pend.compareTo(BigDecimal.ZERO) < 0) {
            pend = BigDecimal.ZERO;
        }
        double progreso = 0.0;
        if (pres.compareTo(BigDecimal.ZERO) > 0) {
            progreso = Math.min(100.0, rec.multiply(new BigDecimal("100")).divide(pres, 2, java.math.RoundingMode.HALF_UP).doubleValue());
        }

        return new ProyectoResponse(
            proyecto.getIdProyecto(),
            proyecto.getCreadoPor(),
            nombreCreador != null ? nombreCreador : (proyecto.getCreadoPor() != null ? "Usuario #" + proyecto.getCreadoPor() : "Sistema"),
            proyecto.getNombre(),
            proyecto.getDescripcion(),
            pres,
            proyecto.getFechaCreacion() == null ? null : proyecto.getFechaCreacion().toString(),
            proyecto.getEstado() == null ? null : proyecto.getEstado().name(),
            rec,
            pend,
            progreso,
            idVotacion,
            tituloVotacion,
            totalAportantes != null ? totalAportantes : 0,
            aportePorMiembro != null ? aportePorMiembro : BigDecimal.ZERO
        );
    }
}