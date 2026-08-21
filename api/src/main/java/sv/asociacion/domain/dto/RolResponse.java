package sv.asociacion.domain.dto;

import sv.asociacion.domain.entity.Rol;

public record RolResponse(Integer idRol, String nombre, String descripcion) {
    public static RolResponse from(Rol rol) {
        return new RolResponse(rol.getIdRol(), rol.getNombre(), rol.getDescripcion());
    }
}
