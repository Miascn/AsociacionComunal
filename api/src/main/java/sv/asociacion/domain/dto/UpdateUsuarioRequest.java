package sv.asociacion.domain.dto;

public record UpdateUsuarioRequest(
    String nombreUsuario,
    String clave,
    Integer idRol,
    Integer idMiembro,
    String estado
) {}