package sv.asociacion.domain.dto;

import java.util.List;

public record BitacoraPageResponse(
    List<BitacoraResponse> items,
    int total,
    int page,
    int size,
    int totalPages
) {
    public static BitacoraPageResponse of(List<BitacoraResponse> items, int total, int page, int size) {
        int totalPages = size <= 0 ? 1 : (int) Math.ceil((double) total / size);
        return new BitacoraPageResponse(items, total, page, size, totalPages);
    }
}
