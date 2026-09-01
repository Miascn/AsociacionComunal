package sv.asociacion.domain.dto;

import java.math.BigDecimal;
import java.util.List;

public record AportacionPageResponse(
    List<AportacionResponse> items,
    int total,
    BigDecimal totalRecaudado,
    int page,
    int size,
    int totalPages
) {
    public static AportacionPageResponse of(List<AportacionResponse> items, int total, BigDecimal totalRecaudado, int page, int size) {
        int totalPages = size <= 0 ? 1 : (int) Math.ceil((double) total / size);
        return new AportacionPageResponse(
            items,
            total,
            totalRecaudado != null ? totalRecaudado : BigDecimal.ZERO,
            page,
            size,
            totalPages
        );
    }
}
