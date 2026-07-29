package sv.asociacion.backend.dao;

import sv.asociacion.backend.config.DBConnection;
import sv.asociacion.backend.entity.Cargo;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CargoDAO implements DAO<Cargo, Integer> {
    @Override
    public Optional<Cargo> findById(Integer id) {
        String sql = "SELECT * FROM cargo WHERE id_cargo = ?";
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
    public List<Cargo> findAll() {
        List<Cargo> cargos = new ArrayList<>();
        String sql = "SELECT * FROM cargo";
        try (Connection conn = DBConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                cargos.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return cargos;
    }

    @Override
    public Cargo save(Cargo entity) {
        String sql = "INSERT INTO cargo (nombre, descripcion) VALUES (?, ?)";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, entity.getNombre());
            ps.setString(2, entity.getDescripcion());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    entity.setIdCargo(keys.getInt(1));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return entity;
    }

    @Override
    public Cargo update(Cargo entity) {
        String sql = "UPDATE cargo SET nombre = ?, descripcion = ? WHERE id_cargo = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, entity.getNombre());
            ps.setString(2, entity.getDescripcion());
            ps.setInt(3, entity.getIdCargo());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return entity;
    }

    @Override
    public boolean delete(Integer id) {
        String sql = "DELETE FROM cargo WHERE id_cargo = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private Cargo mapResultSet(ResultSet rs) throws SQLException {
        return new Cargo(
            rs.getInt("id_cargo"),
            rs.getString("nombre"),
            rs.getString("descripcion")
        );
    }
}
