package sv.asociacion.dao;

import sv.asociacion.config.DBConnection;
import sv.asociacion.domain.entity.PeriodoDirectiva;
import sv.asociacion.util.DateUtils;

import java.sql.*;
import java.time.LocalDate;
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

    public Optional<PeriodoDirectiva> findActivo() {
        String sql = "SELECT * FROM periodo_directiva WHERE estado = 'ACTIVO' ORDER BY id_periodo DESC LIMIT 1";
        try (Connection conn = DBConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return Optional.of(mapResultSet(rs));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public List<PeriodoDirectiva> findFiltered(PeriodoDirectiva.Estado estado, String busqueda) {
        List<PeriodoDirectiva> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM periodo_directiva WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (estado != null) {
            sql.append("AND estado = ? ");
            params.add(estado.name());
        }
        if (busqueda != null && !busqueda.isBlank()) {
            sql.append("AND LOWER(nombre) LIKE ? ");
            params.add("%" + busqueda.trim().toLowerCase() + "%");
        }

        sql.append("ORDER BY fecha_inicio DESC, id_periodo DESC");

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
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

    public List<PeriodoDirectiva> findOverlapping(LocalDate inicio, LocalDate fin, Integer excludeId) {
        List<PeriodoDirectiva> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
            "SELECT * FROM periodo_directiva WHERE fecha_inicio <= ? AND fecha_fin >= ? "
        );
        if (excludeId != null) {
            sql.append("AND id_periodo != ? ");
        }

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            ps.setString(1, DateUtils.formatDate(fin));
            ps.setString(2, DateUtils.formatDate(inicio));
            if (excludeId != null) {
                ps.setInt(3, excludeId);
            }
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

    public void finalizarActivosExcepto(Integer excludeId) {
        StringBuilder sql = new StringBuilder("UPDATE periodo_directiva SET estado = 'FINALIZADO' WHERE estado = 'ACTIVO' ");
        if (excludeId != null) {
            sql.append("AND id_periodo != ?");
        }
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            if (excludeId != null) {
                ps.setInt(1, excludeId);
            }
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean updateEstado(Integer id, PeriodoDirectiva.Estado nuevoEstado) {
        String sql = "UPDATE periodo_directiva SET estado = ? WHERE id_periodo = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nuevoEstado.name());
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public List<PeriodoDirectiva> findAll() {
        return findFiltered(null, null);
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
