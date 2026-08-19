package sv.asociacion.dao;

import sv.asociacion.config.DBConnection;
import sv.asociacion.domain.entity.Votacion;
import sv.asociacion.util.DateUtils;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class VotacionDAO implements DAO<Votacion, Integer> {
    @Override
    public Optional<Votacion> findById(Integer id) {
        String sql = "SELECT * FROM votacion WHERE id_votacion = ?";
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
    public List<Votacion> findAll() {
        List<Votacion> votaciones = new ArrayList<>();
        String sql = "SELECT * FROM votacion";
        try (Connection conn = DBConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                votaciones.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return votaciones;
    }

    @Override
    public Votacion save(Votacion entity) {
        String sql = "INSERT INTO votacion (id_proyecto, titulo, fecha_inicio, fecha_fin, estado) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, entity.getIdProyecto());
            ps.setString(2, entity.getTitulo());
            ps.setString(3, DateUtils.formatDateTime(entity.getFechaInicio()));
            ps.setString(4, DateUtils.formatDateTime(entity.getFechaFin()));
            ps.setString(5, entity.getEstado().name());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    entity.setIdVotacion(keys.getInt(1));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return entity;
    }

    @Override
    public Votacion update(Votacion entity) {
        String sql = "UPDATE votacion SET id_proyecto = ?, titulo = ?, fecha_inicio = ?, fecha_fin = ?, estado = ? WHERE id_votacion = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, entity.getIdProyecto());
            ps.setString(2, entity.getTitulo());
            ps.setString(3, DateUtils.formatDateTime(entity.getFechaInicio()));
            ps.setString(4, DateUtils.formatDateTime(entity.getFechaFin()));
            ps.setString(5, entity.getEstado().name());
            ps.setInt(6, entity.getIdVotacion());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return entity;
    }

    @Override
    public boolean delete(Integer id) {
        String sql = "DELETE FROM votacion WHERE id_votacion = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public List<Votacion> findByProyecto(Integer idProyecto) {
        List<Votacion> list = new ArrayList<>();
        String sql = "SELECT * FROM votacion WHERE id_proyecto = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idProyecto);
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

    private Votacion mapResultSet(ResultSet rs) throws SQLException {
        return new Votacion(
            rs.getInt("id_votacion"),
            rs.getInt("id_proyecto"),
            rs.getString("titulo"),
            DateUtils.parseDateTime(rs.getString("fecha_inicio")),
            DateUtils.parseDateTime(rs.getString("fecha_fin")),
            Votacion.Estado.valueOf(rs.getString("estado"))
        );
    }
}
