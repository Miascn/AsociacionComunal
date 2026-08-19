package sv.asociacion.dao;

import sv.asociacion.config.DBConnection;
import sv.asociacion.domain.entity.Rol;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RolDAO implements DAO<Rol, Integer> {
    @Override
    public Optional<Rol> findById(Integer id) {
        String sql = "SELECT * FROM rol WHERE id_rol = ?";
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
    public List<Rol> findAll() {
        List<Rol> roles = new ArrayList<>();
        String sql = "SELECT * FROM rol";
        try (Connection conn = DBConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                roles.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return roles;
    }

    @Override
    public Rol save(Rol entity) {
        String sql = "INSERT INTO rol (nombre, descripcion) VALUES (?, ?)";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, entity.getNombre());
            ps.setString(2, entity.getDescripcion());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    entity.setIdRol(keys.getInt(1));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return entity;
    }

    @Override
    public Rol update(Rol entity) {
        String sql = "UPDATE rol SET nombre = ?, descripcion = ? WHERE id_rol = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, entity.getNombre());
            ps.setString(2, entity.getDescripcion());
            ps.setInt(3, entity.getIdRol());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return entity;
    }

    @Override
    public boolean delete(Integer id) {
        String sql = "DELETE FROM rol WHERE id_rol = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private Rol mapResultSet(ResultSet rs) throws SQLException {
        return new Rol(
            rs.getInt("id_rol"),
            rs.getString("nombre"),
            rs.getString("descripcion")
        );
    }
}
