package sv.asociacion.domain.dto;

public record LoginResponse(String token, String displayName, String role, Integer idUsuario) {}