package sv.asociacion.domain.dto;

public record ReunionResponse(
    Integer id,
    String titulo,
    String fechaHora,
    String lugar,
    String tipo,
    String estado,
    int totalConvocados,
    int totalAsistentes,
    double porcentajeAsistencia,
    boolean editable
) {}
