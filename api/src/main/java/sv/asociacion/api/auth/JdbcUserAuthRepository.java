package sv.asociacion.api.auth;

import sv.asociacion.backend.config.DBConnection;
import java.sql.*;
import java.util.Optional;

public final class JdbcUserAuthRepository implements UserAuthRepository {
    private static final String SELECT_BASE = """
        SELECT u.id_usuario, u.id_miembro, u.nombre_usuario, u.clave_hash,
               u.estado AS usuario_estado, r.nombre AS rol,
               m.nombres, m.apellidos, m.estado AS miembro_estado
        FROM usuario u
        JOIN rol r ON r.id_rol = u.id_rol
        LEFT JOIN miembro m ON m.id_miembro = u.id_miembro
        """;

    @Override
    public Optional<AuthUser> findByUsername(String username) {
        return find(SELECT_BASE + " WHERE LOWER(u.nombre_usuario) = ?", statement -> statement.setString(1, username));
    }

    @Override
    public Optional<AuthUser> findById(int id) {
        return find(SELECT_BASE + " WHERE u.id_usuario = ?", statement -> statement.setInt(1, id));
    }

    @Override
    public void updateLastAccess(int id) {
        try (Connection connection = DBConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(
                 "UPDATE usuario SET ultimo_acceso = CURRENT_TIMESTAMP WHERE id_usuario = ?")) {
            statement.setInt(1, id);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("No fue posible actualizar el acceso del usuario.", exception);
        }
    }

    private Optional<AuthUser> find(String sql, SqlBinder binder) {
        try (Connection connection = DBConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            binder.bind(statement);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(map(result)) : Optional.empty();
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("No fue posible consultar la identidad del usuario.", exception);
        }
    }

    private AuthUser map(ResultSet result) throws SQLException {
        return new AuthUser(
            result.getInt("id_usuario"), result.getObject("id_miembro", Integer.class),
            result.getString("nombre_usuario"), result.getString("clave_hash"),
            result.getString("usuario_estado"), result.getString("rol"),
            result.getString("nombres"), result.getString("apellidos"),
            result.getString("miembro_estado")
        );
    }

    @FunctionalInterface private interface SqlBinder { void bind(PreparedStatement statement) throws SQLException; }
}
