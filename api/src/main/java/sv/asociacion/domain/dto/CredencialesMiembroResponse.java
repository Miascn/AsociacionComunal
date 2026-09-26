package sv.asociacion.domain.dto;

public record CredencialesMiembroResponse(
    Integer idMiembro,
    String nombreUsuario,
    String claveTemporal,
    boolean requiereCambioClave,
    String mensaje
) {}
