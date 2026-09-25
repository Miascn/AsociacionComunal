package sv.asociacion.domain.dto;

public record ReunionRequest(
    String titulo,
    String fechaHora,
    String lugar,
    String tipo,
    String estado,
    Integer idProyecto,
    String descripcion
) {
    public ReunionRequest(String titulo, String fechaHora, String lugar, String tipo, String estado) {
        this(titulo, fechaHora, lugar, tipo, estado, null, null);
    }
}
