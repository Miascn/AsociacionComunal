package sv.asociacion.api.auth;

public record AuthUser(
    int id,
    Integer memberId,
    String username,
    String passwordHash,
    String userStatus,
    String role,
    String memberNames,
    String memberLastNames,
    String memberStatus
) { }
