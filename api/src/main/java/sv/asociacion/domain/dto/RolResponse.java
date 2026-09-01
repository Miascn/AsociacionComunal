package sv.asociacion.domain.dto;

import sv.asociacion.domain.entity.Rol;

public record RolResponse(Integer idRol, String nombre, String descripcion, Integer usuariosAsociados) {
    public static RolResponse from(Rol rol) {
        return from(rol, 0);
    }

    public static RolResponse from(Rol rol, int usuariosAsociados) {
        return new RolResponse(rol.getIdRol(), rol.getNombre(), rol.getDescripcion(), usuariosAsociados);
    }
}
