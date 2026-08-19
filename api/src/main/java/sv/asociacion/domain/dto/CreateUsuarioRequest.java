package sv.asociacion.domain.dto;

public record CreateUsuarioRequest(
    String nombreUsuario,
    String clave,
    Integer idRol,
    Integer idMiembro
) {}