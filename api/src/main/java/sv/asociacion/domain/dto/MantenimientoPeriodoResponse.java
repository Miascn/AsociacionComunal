package sv.asociacion.domain.dto;

import java.math.BigDecimal;
import java.util.List;

public record MantenimientoPeriodoResponse(
    String periodo,
    BigDecimal cuotaMonto,
    int totalMiembros,
    int totalPagados,
    int totalPendientes,
    BigDecimal totalRecaudado,
    BigDecimal totalEsperado,
    double porcentajePagado,
    List<Item> items
) {
    public record Item(
        Integer idMiembro,
        String dui,
        String nombreCompleto,
        String vivienda,
        boolean pagado,
        Long idAportacion,
        BigDecimal monto,
        String fechaPago,
        String metodoPago,
        String referencia,
        String estadoAportacion
    ) {}
}
