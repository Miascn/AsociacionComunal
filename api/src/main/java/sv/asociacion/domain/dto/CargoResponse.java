package sv.asociacion.domain.dto;

import sv.asociacion.domain.entity.Cargo;

public record CargoResponse(
    Integer id,
    String nombre,
    String descripcion,
    Integer nivelJerarquico,
    Boolean activo,
    Integer totalAsignaciones
) {
    public static CargoResponse from(Cargo cargo) {
        return from(cargo, 0);
    }

    public static CargoResponse from(Cargo cargo, int totalAsignaciones) {
        return new CargoResponse(
            cargo.getIdCargo(),
            cargo.getNombre(),
            cargo.getDescripcion(),
            cargo.getNivelJerarquico() != null ? cargo.getNivelJerarquico() : 1,
            cargo.getActivo() != null ? cargo.getActivo() : true,
            totalAsignaciones
        );
    }
}
