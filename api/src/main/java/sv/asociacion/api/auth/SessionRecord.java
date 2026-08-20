package sv.asociacion.api.auth;

import java.time.Instant;

public record SessionRecord(
    long id,
    int userId,
    String accessTokenHash,
    String refreshTokenHash,
    Instant createdAt,
    Instant accessExpiresAt,
    Instant refreshExpiresAt,
    Instant revokedAt
) {
    public boolean revoked() { return revokedAt != null; }
}
