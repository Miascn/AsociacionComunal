package sv.asociacion.domain.dto;

public record ReunionRequest(
    String titulo,
    String fechaHora,
    String lugar,
    String tipo,
    String estado
) {}
