package sv.asociacion.domain.dto;

public record UsuarioResponse(
    Integer idUsuario,
    String nombreUsuario,
    Integer idRol,
    Integer idMiembro,
    String estado,
    String ultimoAcceso,
    Boolean requiereCambioClave,
    String claveTemporal,
    String tipoAcceso
) {
    public UsuarioResponse(Integer idUsuario, String nombreUsuario, Integer idRol, Integer idMiembro,
                           String estado, String ultimoAcceso, Boolean requiereCambioClave, String claveTemporal) {
        this(idUsuario, nombreUsuario, idRol, idMiembro, estado, ultimoAcceso, requiereCambioClave, claveTemporal, "SISTEMA_JAVA");
    }
}