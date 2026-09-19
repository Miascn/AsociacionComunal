package sv.asociacion.dao;

import sv.asociacion.config.DBConnection;
import sv.asociacion.domain.entity.Usuario;
import sv.asociacion.util.DateUtils;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UsuarioDAO implements DAO<Usuario, Integer> {
    @Override
    public Optional<Usuario> findById(Integer id) {
        String sql = "SELECT * FROM usuario WHERE id_usuario = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapResultSet(rs));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("No fue posible crear el usuario.", e);
        }
        return Optional.empty();
    }

    @Override
    public List<Usuario> findAll() {
        List<Usuario> usuarios = new ArrayList<>();
        String sql = "SELECT * FROM usuario";
        try (Connection conn = DBConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                usuarios.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return usuarios;
    }

    @Override
    public Usuario save(Usuario entity) {
        String sql = "INSERT INTO usuario (id_rol, id_miembro, nombre_usuario, clave_hash, estado, ultimo_acceso) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, entity.getIdRol());
            if (entity.getIdMiembro() != null) {
                ps.setInt(2, entity.getIdMiembro());
            } else {
                ps.setNull(2, Types.INTEGER);
            }
            ps.setString(3, entity.getNombreUsuario());
            ps.setString(4, entity.getClaveHash());
            ps.setString(5, entity.getEstado().name());
            ps.setString(6, DateUtils.formatDateTime(entity.getUltimoAcceso()));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    entity.setIdUsuario(keys.getInt(1));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return entity;
    }

    @Override
    public Usuario update(Usuario entity) {
        String sql = "UPDATE usuario SET id_rol = ?, id_miembro = ?, nombre_usuario = ?, clave_hash = ?, estado = ?, ultimo_acceso = ? WHERE id_usuario = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, entity.getIdRol());
            if (entity.getIdMiembro() != null) {
                ps.setInt(2, entity.getIdMiembro());
            } else {
                ps.setNull(2, Types.INTEGER);
            }
            ps.setString(3, entity.getNombreUsuario());
            ps.setString(4, entity.getClaveHash());
            ps.setString(5, entity.getEstado().name());
            ps.setString(6, DateUtils.formatDateTime(entity.getUltimoAcceso()));
            ps.setInt(7, entity.getIdUsuario());
            if (ps.executeUpdate() == 0) return null;
        } catch (SQLException e) {
            throw new IllegalStateException("No fue posible actualizar el usuario.", e);
        }
        return entity;
    }

    @Override
    public boolean delete(Integer id) {
        String sql = "UPDATE usuario SET estado = 'INACTIVO' WHERE id_usuario = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IllegalStateException("No fue posible desactivar el usuario.", e);
        }
    }

    public boolean resetPassword(Integer id, String claveHash, boolean requiereCambioClave) {
        String sql = "UPDATE usuario SET clave_hash = ?, requiere_cambio_clave = ? WHERE id_usuario = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, claveHash);
            ps.setBoolean(2, requiereCambioClave);
            ps.setInt(3, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IllegalStateException("No fue posible restablecer la contraseña.", e);
        }
    }

    public Usuario findByNombreUsuario(String nombreUsuario) {
        String sql = "SELECT * FROM usuario WHERE nombre_usuario = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nombreUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapResultSet(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public int countByRol(Integer idRol) {
        if (idRol == null) return 0;
        String sql = "SELECT COUNT(*) FROM usuario WHERE id_rol = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idRol);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Error al consultar usuarios por rol.", e);
        }
        return 0;
    }

    private Usuario mapResultSet(ResultSet rs) throws SQLException {
        Integer idMiembro = rs.getObject("id_miembro", Integer.class);
        return new Usuario(
            rs.getInt("id_usuario"),
            rs.getInt("id_rol"),
            idMiembro,
            rs.getString("nombre_usuario"),
            rs.getString("clave_hash"),
            Usuario.Estado.valueOf(rs.getString("estado")),
            DateUtils.parseDateTime(rs.getString("ultimo_acceso"))
        );
    }
}
