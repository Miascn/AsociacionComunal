package sv.asociacion.api.auth;

public record AuthPrincipal(
    long sessionId,
    int userId,
    Integer memberId,
    String username,
    String role,
    boolean passwordChangeRequired,
    String memberNames,
    String memberLastNames,
    String memberStatus
) { }
