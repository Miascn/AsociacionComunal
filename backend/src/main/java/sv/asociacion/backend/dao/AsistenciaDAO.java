package sv.asociacion.backend.dao;

import sv.asociacion.backend.config.DBConnection;
import sv.asociacion.backend.entity.Asistencia;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AsistenciaDAO implements DAO<Asistencia, Long> {
    @Override
    public Optional<Asistencia> findById(Long id) {
        String sql = "SELECT * FROM asistencia WHERE id_asistencia = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapResultSet(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    @Override
    public List<Asistencia> findAll() {
        List<Asistencia> asignaciones = new ArrayList<>();
        String sql = "SELECT * FROM asistencia";
        try (Connection conn = DBConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                asignaciones.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return asignaciones;
    }

    @Override
    public Asistencia save(Asistencia entity) {
        String sql = "INSERT INTO asistencia (id_reunion, id_miembro, asistio, observacion) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, entity.getIdReunion());
            ps.setInt(2, entity.getIdMiembro());
            ps.setBoolean(3, entity.isAsistio());
            ps.setString(4, entity.getObservacion());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    entity.setIdAsistencia(keys.getLong(1));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return entity;
    }

    @Override
    public Asistencia update(Asistencia entity) {
        String sql = "UPDATE asistencia SET id_reunion = ?, id_miembro = ?, asistio = ?, observacion = ? WHERE id_asistencia = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, entity.getIdReunion());
            ps.setInt(2, entity.getIdMiembro());
            ps.setBoolean(3, entity.isAsistio());
            ps.setString(4, entity.getObservacion());
            ps.setLong(5, entity.getIdAsistencia());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return entity;
    }

    @Override
    public boolean delete(Long id) {
        String sql = "DELETE FROM asistencia WHERE id_asistencia = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public List<Asistencia> findByReunion(Integer idReunion) {
        List<Asistencia> list = new ArrayList<>();
        String sql = "SELECT * FROM asistencia WHERE id_reunion = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idReunion);
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

    public List<Asistencia> findByMiembro(Integer idMiembro) {
        List<Asistencia> list = new ArrayList<>();
        String sql = "SELECT * FROM asistencia WHERE id_miembro = ?";
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

    private Asistencia mapResultSet(ResultSet rs) throws SQLException {
        return new Asistencia(
            rs.getLong("id_asistencia"),
            rs.getInt("id_reunion"),
            rs.getInt("id_miembro"),
            rs.getBoolean("asistio"),
            rs.getString("observacion")
        );
    }
}
