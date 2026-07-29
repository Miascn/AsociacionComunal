package sv.asociacion.backend.dao;

import sv.asociacion.backend.config.DBConnection;
import sv.asociacion.backend.entity.PeriodoDirectiva;
import sv.asociacion.backend.util.DateUtils;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PeriodoDirectivaDAO implements DAO<PeriodoDirectiva, Integer> {
    @Override
    public Optional<PeriodoDirectiva> findById(Integer id) {
        String sql = "SELECT * FROM periodo_directiva WHERE id_periodo = ?";
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
    public List<PeriodoDirectiva> findAll() {
        List<PeriodoDirectiva> periodos = new ArrayList<>();
        String sql = "SELECT * FROM periodo_directiva";
        try (Connection conn = DBConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                periodos.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return periodos;
    }

    @Override
    public PeriodoDirectiva save(PeriodoDirectiva entity) {
        String sql = "INSERT INTO periodo_directiva (nombre, fecha_inicio, fecha_fin, estado) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, entity.getNombre());
            ps.setString(2, DateUtils.formatDate(entity.getFechaInicio()));
            ps.setString(3, DateUtils.formatDate(entity.getFechaFin()));
            ps.setString(4, entity.getEstado().name());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    entity.setIdPeriodo(keys.getInt(1));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return entity;
    }

    @Override
    public PeriodoDirectiva update(PeriodoDirectiva entity) {
        String sql = "UPDATE periodo_directiva SET nombre = ?, fecha_inicio = ?, fecha_fin = ?, estado = ? WHERE id_periodo = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, entity.getNombre());
            ps.setString(2, DateUtils.formatDate(entity.getFechaInicio()));
            ps.setString(3, DateUtils.formatDate(entity.getFechaFin()));
            ps.setString(4, entity.getEstado().name());
            ps.setInt(5, entity.getIdPeriodo());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return entity;
    }

    @Override
    public boolean delete(Integer id) {
        String sql = "DELETE FROM periodo_directiva WHERE id_periodo = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private PeriodoDirectiva mapResultSet(ResultSet rs) throws SQLException {
        return new PeriodoDirectiva(
            rs.getInt("id_periodo"),
            rs.getString("nombre"),
            DateUtils.parseDate(rs.getString("fecha_inicio")),
            DateUtils.parseDate(rs.getString("fecha_fin")),
            PeriodoDirectiva.Estado.valueOf(rs.getString("estado"))
        );
    }
}
