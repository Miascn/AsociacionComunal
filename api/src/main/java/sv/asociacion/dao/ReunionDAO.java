package sv.asociacion.dao;

import sv.asociacion.config.DBConnection;
import sv.asociacion.domain.entity.Reunion;
import sv.asociacion.util.DateUtils;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ReunionDAO implements DAO<Reunion, Integer> {
    @Override
    public Optional<Reunion> findById(Integer id) {
        String sql = "SELECT * FROM reunion WHERE id_reunion = ?";
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
    public List<Reunion> findAll() {
        List<Reunion> reuniones = new ArrayList<>();
        String sql = "SELECT * FROM reunion";
        try (Connection conn = DBConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                reuniones.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return reuniones;
    }

    @Override
    public Reunion save(Reunion entity) {
        String sql = "INSERT INTO reunion (titulo, fecha_hora, lugar, tipo, estado) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, entity.getTitulo());
            ps.setString(2, DateUtils.formatDateTime(entity.getFechaHora()));
            ps.setString(3, entity.getLugar());
            ps.setString(4, entity.getTipo().name());
            ps.setString(5, entity.getEstado().name());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    entity.setIdReunion(keys.getInt(1));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return entity;
    }

    @Override
    public Reunion update(Reunion entity) {
        String sql = "UPDATE reunion SET titulo = ?, fecha_hora = ?, lugar = ?, tipo = ?, estado = ? WHERE id_reunion = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, entity.getTitulo());
            ps.setString(2, DateUtils.formatDateTime(entity.getFechaHora()));
            ps.setString(3, entity.getLugar());
            ps.setString(4, entity.getTipo().name());
            ps.setString(5, entity.getEstado().name());
            ps.setInt(6, entity.getIdReunion());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return entity;
    }

    @Override
    public boolean delete(Integer id) {
        String sql = "DELETE FROM reunion WHERE id_reunion = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public List<Reunion> findByEstado(Reunion.Estado estado) {
        List<Reunion> list = new ArrayList<>();
        String sql = "SELECT * FROM reunion WHERE estado = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, estado.name());
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

    private Reunion mapResultSet(ResultSet rs) throws SQLException {
        return new Reunion(
            rs.getInt("id_reunion"),
            rs.getString("titulo"),
            DateUtils.parseDateTime(rs.getString("fecha_hora")),
            rs.getString("lugar"),
            Reunion.Tipo.valueOf(rs.getString("tipo")),
            Reunion.Estado.valueOf(rs.getString("estado"))
        );
    }
}
