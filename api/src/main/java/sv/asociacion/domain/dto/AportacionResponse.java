package sv.asociacion.domain.dto;

import java.math.BigDecimal;
import sv.asociacion.domain.entity.Aportacion;
import sv.asociacion.util.DateUtils;

public record AportacionResponse(
    Long idAportacion,
    Integer idMiembro,
    String nombreMiembro,
    String duiMiembro,
    Integer idProyecto,
    String nombreProyecto,
    String periodoMes,
    BigDecimal monto,
    String fechaPago,
    String metodoPago,
    String referencia,
    String estado
) {
    public static AportacionResponse from(Aportacion a, String nombreMiembro, String duiMiembro, String nombreProyecto) {
        return new AportacionResponse(
            a.getIdAportacion(),
            a.getIdMiembro(),
            nombreMiembro != null ? nombreMiembro : "Miembro #" + a.getIdMiembro(),
            duiMiembro != null ? duiMiembro : "-",
            a.getIdProyecto(),
            nombreProyecto != null ? nombreProyecto : (a.getIdProyecto() != null ? "Proyecto #" + a.getIdProyecto() : null),
            a.getPeriodoMes(),
            a.getMonto(),
            a.getFechaPago() != null ? DateUtils.formatDate(a.getFechaPago()) : null,
            a.getMetodoPago() != null ? a.getMetodoPago().name() : "OTRO",
            a.getReferencia(),
            a.getEstado() != null ? a.getEstado().name() : "REGISTRADA"
        );
    }
}
