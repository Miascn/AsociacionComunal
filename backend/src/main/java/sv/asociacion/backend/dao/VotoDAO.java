package sv.asociacion.backend.dao;

import sv.asociacion.backend.config.DBConnection;
import sv.asociacion.backend.entity.Voto;
import sv.asociacion.backend.util.DateUtils;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class VotoDAO implements DAO<Voto, Long> {
    @Override
    public Optional<Voto> findById(Long id) {
        String sql = "SELECT * FROM voto WHERE id_voto = ?";
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
    public List<Voto> findAll() {
        List<Voto> votos = new ArrayList<>();
        String sql = "SELECT * FROM voto";
        try (Connection conn = DBConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                votos.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return votos;
    }

    @Override
    public Voto save(Voto entity) {
        String sql = "INSERT INTO voto (id_votacion, id_opcion, id_miembro, fecha_hora) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, entity.getIdVotacion());
            ps.setInt(2, entity.getIdOpcion());
            ps.setInt(3, entity.getIdMiembro());
            ps.setString(4, DateUtils.formatDateTime(entity.getFechaHora()));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    entity.setIdVoto(keys.getLong(1));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return entity;
    }

    @Override
    public Voto update(Voto entity) {
        String sql = "UPDATE voto SET id_votacion = ?, id_opcion = ?, id_miembro = ?, fecha_hora = ? WHERE id_voto = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, entity.getIdVotacion());
            ps.setInt(2, entity.getIdOpcion());
            ps.setInt(3, entity.getIdMiembro());
            ps.setString(4, DateUtils.formatDateTime(entity.getFechaHora()));
            ps.setLong(5, entity.getIdVoto());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return entity;
    }

    @Override
    public boolean delete(Long id) {
        String sql = "DELETE FROM voto WHERE id_voto = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public List<Voto> findByVotacion(Integer idVotacion) {
        List<Voto> list = new ArrayList<>();
        String sql = "SELECT * FROM voto WHERE id_votacion = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idVotacion);
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

    public List<Voto> findByOpcion(Integer idOpcion) {
        List<Voto> list = new ArrayList<>();
        String sql = "SELECT * FROM voto WHERE id_opcion = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idOpcion);
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

    public boolean existsByVotacionAndMiembro(Integer idVotacion, Integer idMiembro) {
        String sql = "SELECT 1 FROM voto WHERE id_votacion = ? AND id_miembro = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idVotacion);
            ps.setInt(2, idMiembro);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private Voto mapResultSet(ResultSet rs) throws SQLException {
        return new Voto(
            rs.getLong("id_voto"),
            rs.getInt("id_votacion"),
            rs.getInt("id_opcion"),
            rs.getInt("id_miembro"),
            DateUtils.parseDateTime(rs.getString("fecha_hora"))
        );
    }
}
