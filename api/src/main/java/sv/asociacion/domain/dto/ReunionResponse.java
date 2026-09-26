package sv.asociacion.domain.dto;

public record ReunionResponse(
    Integer id,
    Integer idProyecto,
    String nombreProyecto,
    String titulo,
    String descripcion,
    String fechaHora,
    String lugar,
    String tipo,
    String estado,
    int totalConvocados,
    int totalAsistentes,
    double porcentajeAsistencia,
    boolean editable
) {
    public ReunionResponse(
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
    ) {
        this(id, null, null, titulo, null, fechaHora, lugar, tipo, estado, totalConvocados, totalAsistentes, porcentajeAsistencia, editable);
    }
}
