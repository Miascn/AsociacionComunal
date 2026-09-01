package sv.asociacion.dao;

import sv.asociacion.config.DBConnection;
import sv.asociacion.domain.entity.OpcionVotacion;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class OpcionVotacionDAO implements DAO<OpcionVotacion, Integer> {
    @Override
    public Optional<OpcionVotacion> findById(Integer id) {
        String sql = "SELECT * FROM opcion_votacion WHERE id_opcion = ?";
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
    public List<OpcionVotacion> findAll() {
        List<OpcionVotacion> opciones = new ArrayList<>();
        String sql = "SELECT * FROM opcion_votacion ORDER BY orden";
        try (Connection conn = DBConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                opciones.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return opciones;
    }

    @Override
    public OpcionVotacion save(OpcionVotacion entity) {
        String sql = "INSERT INTO opcion_votacion (id_votacion, descripcion, orden) VALUES (?, ?, ?)";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, entity.getIdVotacion());
            ps.setString(2, entity.getDescripcion());
            ps.setShort(3, entity.getOrden());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    entity.setIdOpcion(keys.getInt(1));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return entity;
    }

    @Override
    public OpcionVotacion update(OpcionVotacion entity) {
        String sql = "UPDATE opcion_votacion SET id_votacion = ?, descripcion = ?, orden = ? WHERE id_opcion = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, entity.getIdVotacion());
            ps.setString(2, entity.getDescripcion());
            ps.setShort(3, entity.getOrden());
            ps.setInt(4, entity.getIdOpcion());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return entity;
    }

    @Override
    public boolean delete(Integer id) {
        String sql = "DELETE FROM opcion_votacion WHERE id_opcion = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public List<OpcionVotacion> findByVotacion(Integer idVotacion) {
        List<OpcionVotacion> opciones = new ArrayList<>();
        String sql = "SELECT * FROM opcion_votacion WHERE id_votacion = ? ORDER BY orden";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idVotacion);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    opciones.add(mapResultSet(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return opciones;
    }

    public int countByVotacion(Integer idVotacion) {
        String sql = "SELECT COUNT(*) FROM opcion_votacion WHERE id_votacion = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idVotacion);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public short findMaxOrdenByVotacion(Integer idVotacion) {
        String sql = "SELECT COALESCE(MAX(orden), 0) FROM opcion_votacion WHERE id_votacion = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idVotacion);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getShort(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    private OpcionVotacion mapResultSet(ResultSet rs) throws SQLException {
        return new OpcionVotacion(
            rs.getInt("id_opcion"),
            rs.getInt("id_votacion"),
            rs.getString("descripcion"),
            rs.getShort("orden")
        );
    }
}
