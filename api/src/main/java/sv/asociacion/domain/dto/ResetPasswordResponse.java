package sv.asociacion.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ResetPasswordResponse(
    Integer idUsuario,
    String nombreUsuario,
    String temporaryPassword
) {
    @JsonProperty("claveTemporal")
    public String claveTemporal() {
        return temporaryPassword;
    }
}
