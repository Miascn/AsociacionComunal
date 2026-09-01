package sv.asociacion.domain.dto;

public record CargoRequest(
    String nombre,
    String descripcion,
    Integer nivelJerarquico,
    Boolean activo
) {}
