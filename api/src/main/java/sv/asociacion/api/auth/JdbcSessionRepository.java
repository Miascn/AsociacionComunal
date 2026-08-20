package sv.asociacion.api.auth;

import sv.asociacion.backend.config.DBConnection;
import java.sql.*;
import java.time.Instant;
import java.util.Optional;

public final class JdbcSessionRepository implements SessionRepository {
    @Override
    public SessionRecord create(int userId, String accessHash, String refreshHash, Instant createdAt,
                                Instant accessExpiresAt, Instant refreshExpiresAt) {
        String sql = "INSERT INTO sesion_usuario (id_usuario, access_token_hash, refresh_token_hash, creada_en, access_expira_en, refresh_expira_en) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection connection = DBConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, userId); statement.setString(2, accessHash); statement.setString(3, refreshHash);
            statement.setTimestamp(4, Timestamp.from(createdAt)); statement.setTimestamp(5, Timestamp.from(accessExpiresAt));
            statement.setTimestamp(6, Timestamp.from(refreshExpiresAt)); statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) throw new SQLException("La sesión no devolvió identificador.");
                return new SessionRecord(keys.getLong(1), userId, accessHash, refreshHash, createdAt, accessExpiresAt, refreshExpiresAt, null);
            }
        } catch (SQLException exception) { throw new IllegalStateException("No fue posible crear la sesión.", exception); }
    }

    @Override public Optional<SessionRecord> findByAccessHash(String hash) { return find("access_token_hash", hash); }
    @Override public Optional<SessionRecord> findByRefreshHash(String hash) { return find("refresh_token_hash", hash); }

    private Optional<SessionRecord> find(String column, String hash) {
        String sql = "SELECT * FROM sesion_usuario WHERE " + column + " = ?";
        try (Connection connection = DBConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, hash);
            try (ResultSet result = statement.executeQuery()) { return result.next() ? Optional.of(map(result)) : Optional.empty(); }
        } catch (SQLException exception) { throw new IllegalStateException("No fue posible consultar la sesión.", exception); }
    }

    @Override
    public boolean rotate(long sessionId, String expectedRefreshHash, String accessHash, String refreshHash, Instant accessExpiresAt, Instant refreshExpiresAt) {
        String sql = "UPDATE sesion_usuario SET access_token_hash=?, refresh_token_hash=?, access_expira_en=?, refresh_expira_en=? WHERE id_sesion=? AND refresh_token_hash=? AND revocada_en IS NULL AND refresh_expira_en > CURRENT_TIMESTAMP";
        return execute(sql, statement -> { statement.setString(1, accessHash); statement.setString(2, refreshHash); statement.setTimestamp(3, Timestamp.from(accessExpiresAt)); statement.setTimestamp(4, Timestamp.from(refreshExpiresAt)); statement.setLong(5, sessionId); statement.setString(6, expectedRefreshHash); }) == 1;
    }

    @Override
    public void revoke(long sessionId, Instant revokedAt) {
        execute("UPDATE sesion_usuario SET revocada_en=? WHERE id_sesion=? AND revocada_en IS NULL", statement -> { statement.setTimestamp(1, Timestamp.from(revokedAt)); statement.setLong(2, sessionId); });
    }

    @Override public void revokeAllForUser(int userId, Instant revokedAt) {
        execute("UPDATE sesion_usuario SET revocada_en=? WHERE id_usuario=? AND revocada_en IS NULL", statement -> { statement.setTimestamp(1, Timestamp.from(revokedAt)); statement.setInt(2, userId); });
    }

    private int execute(String sql, SqlBinder binder) {
        try (Connection connection = DBConnection.getInstance().getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            binder.bind(statement); return statement.executeUpdate();
        } catch (SQLException exception) { throw new IllegalStateException("No fue posible actualizar la sesión.", exception); }
    }

    private SessionRecord map(ResultSet result) throws SQLException {
        Timestamp revoked = result.getTimestamp("revocada_en");
        return new SessionRecord(result.getLong("id_sesion"), result.getInt("id_usuario"), result.getString("access_token_hash"),
            result.getString("refresh_token_hash"), result.getTimestamp("creada_en").toInstant(), result.getTimestamp("access_expira_en").toInstant(),
            result.getTimestamp("refresh_expira_en").toInstant(), revoked == null ? null : revoked.toInstant());
    }

    @FunctionalInterface private interface SqlBinder { void bind(PreparedStatement statement) throws SQLException; }
}
