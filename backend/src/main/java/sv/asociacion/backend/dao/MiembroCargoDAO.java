package sv.asociacion.backend.dao;

import sv.asociacion.backend.config.DBConnection;
import sv.asociacion.backend.entity.MiembroCargo;
import sv.asociacion.backend.util.DateUtils;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MiembroCargoDAO implements DAO<MiembroCargo, Integer> {
    @Override
    public Optional<MiembroCargo> findById(Integer id) {
        String sql = "SELECT * FROM miembro_cargo WHERE id_miembro_cargo = ?";
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
    public List<MiembroCargo> findAll() {
        List<MiembroCargo> miembrosCargos = new ArrayList<>();
        String sql = "SELECT * FROM miembro_cargo";
        try (Connection conn = DBConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                miembrosCargos.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return miembrosCargos;
    }

    @Override
    public MiembroCargo save(MiembroCargo entity) {
        String sql = "INSERT INTO miembro_cargo (id_miembro, id_cargo, id_periodo, fecha_asignacion) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, entity.getIdMiembro());
            ps.setInt(2, entity.getIdCargo());
            ps.setInt(3, entity.getIdPeriodo());
            ps.setString(4, DateUtils.formatDate(entity.getFechaAsignacion()));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    entity.setIdMiembroCargo(keys.getInt(1));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return entity;
    }

    @Override
    public MiembroCargo update(MiembroCargo entity) {
        String sql = "UPDATE miembro_cargo SET id_miembro = ?, id_cargo = ?, id_periodo = ?, fecha_asignacion = ? WHERE id_miembro_cargo = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, entity.getIdMiembro());
            ps.setInt(2, entity.getIdCargo());
            ps.setInt(3, entity.getIdPeriodo());
            ps.setString(4, DateUtils.formatDate(entity.getFechaAsignacion()));
            ps.setInt(5, entity.getIdMiembroCargo());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return entity;
    }

    @Override
    public boolean delete(Integer id) {
        String sql = "DELETE FROM miembro_cargo WHERE id_miembro_cargo = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public List<MiembroCargo> findByMiembro(Integer idMiembro) {
        List<MiembroCargo> list = new ArrayList<>();
        String sql = "SELECT * FROM miembro_cargo WHERE id_miembro = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idMiembro);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSet(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<MiembroCargo> findByPeriodo(Integer idPeriodo) {
        List<MiembroCargo> list = new ArrayList<>();
        String sql = "SELECT * FROM miembro_cargo WHERE id_periodo = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idPeriodo);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSet(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    private MiembroCargo mapResultSet(ResultSet rs) throws SQLException {
        return new MiembroCargo(
            rs.getInt("id_miembro_cargo"),
            rs.getInt("id_miembro"),
            rs.getInt("id_cargo"),
            rs.getInt("id_periodo"),
            DateUtils.parseDate(rs.getString("fecha_asignacion"))
        );
    }
}
