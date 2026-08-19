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
            e.printStackTrace();
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
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return entity;
    }

    @Override
    public boolean delete(Integer id) {
        String sql = "DELETE FROM usuario WHERE id_usuario = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
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
