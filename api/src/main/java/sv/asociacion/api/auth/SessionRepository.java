package sv.asociacion.api.auth;

import java.time.Instant;
import java.util.Optional;

public interface SessionRepository {
    SessionRecord create(int userId, String accessHash, String refreshHash,
                         Instant createdAt, Instant accessExpiresAt, Instant refreshExpiresAt);
    Optional<SessionRecord> findByAccessHash(String hash);
    Optional<SessionRecord> findByRefreshHash(String hash);
    boolean rotate(long sessionId, String expectedRefreshHash, String accessHash, String refreshHash,
                   Instant accessExpiresAt, Instant refreshExpiresAt);
    void revoke(long sessionId, Instant revokedAt);
}
