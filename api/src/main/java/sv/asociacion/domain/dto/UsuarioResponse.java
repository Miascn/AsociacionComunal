package sv.asociacion.domain.dto;

public record UsuarioResponse(
    Integer idUsuario,
    String nombreUsuario,
    Integer idRol,
    Integer idMiembro,
    String estado,
    String ultimoAcceso
) {}