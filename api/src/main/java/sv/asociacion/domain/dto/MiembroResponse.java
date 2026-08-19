package sv.asociacion.domain.dto;

import sv.asociacion.domain.entity.Miembro;

public record MiembroResponse(
    Integer id,
    String dui,
    String nombres,
    String apellidos,
    String telefono,
    String correo,
    String direccion,
    String fechaIngreso,
    String estado
) {
    public static MiembroResponse from(Miembro miembro) {
        return new MiembroResponse(
            miembro.getIdMiembro(),
            miembro.getDui(),
            miembro.getNombres(),
            miembro.getApellidos(),
            miembro.getTelefono(),
            miembro.getCorreo(),
            miembro.getDireccion(),
            miembro.getFechaIngreso() == null ? null : miembro.getFechaIngreso().toString(),
            miembro.getEstado() == null ? null : miembro.getEstado().name()
        );
    }
}