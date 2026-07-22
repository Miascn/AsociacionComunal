package sv.asociacion.backend.dao;

import sv.asociacion.backend.config.DBConnection;
import sv.asociacion.backend.entity.Bitacora;
import sv.asociacion.backend.util.DateUtils;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class BitacoraDAO implements DAO<Bitacora, Long> {
    @Override
    public Optional<Bitacora> findById(Long id) {
        String sql = "SELECT * FROM bitacora WHERE id_bitacora = ?";
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
    public List<Bitacora> findAll() {
        List<Bitacora> bitacoras = new ArrayList<>();
        String sql = "SELECT * FROM bitacora ORDER BY fecha_hora DESC";
        try (Connection conn = DBConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                bitacoras.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return bitacoras;
    }

    @Override
    public Bitacora save(Bitacora entity) {
        String sql = "INSERT INTO bitacora (id_usuario, accion, entidad, id_registro, fecha_hora, detalle) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, entity.getIdUsuario());
            ps.setString(2, entity.getAccion());
            ps.setString(3, entity.getEntidad());
            ps.setString(4, entity.getIdRegistro());
            ps.setString(5, DateUtils.formatDateTime(entity.getFechaHora()));
            ps.setString(6, entity.getDetalle());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    entity.setIdBitacora(keys.getLong(1));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return entity;
    }

    @Override
    public Bitacora update(Bitacora entity) {
        String sql = "UPDATE bitacora SET id_usuario = ?, accion = ?, entidad = ?, id_registro = ?, fecha_hora = ?, detalle = ? WHERE id_bitacora = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, entity.getIdUsuario());
            ps.setString(2, entity.getAccion());
            ps.setString(3, entity.getEntidad());
            ps.setString(4, entity.getIdRegistro());
            ps.setString(5, DateUtils.formatDateTime(entity.getFechaHora()));
            ps.setString(6, entity.getDetalle());
            ps.setLong(7, entity.getIdBitacora());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return entity;
    }

    @Override
    public boolean delete(Long id) {
        String sql = "DELETE FROM bitacora WHERE id_bitacora = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public List<Bitacora> findByUsuario(Integer idUsuario) {
        List<Bitacora> list = new ArrayList<>();
        String sql = "SELECT * FROM bitacora WHERE id_usuario = ? ORDER BY fecha_hora DESC";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idUsuario);
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

    public List<Bitacora> findByEntidad(String entidad) {
        List<Bitacora> list = new ArrayList<>();
        String sql = "SELECT * FROM bitacora WHERE entidad = ? ORDER BY fecha_hora DESC";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, entidad);
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

    private Bitacora mapResultSet(ResultSet rs) throws SQLException {
        return new Bitacora(
            rs.getLong("id_bitacora"),
            rs.getInt("id_usuario"),
            rs.getString("accion"),
            rs.getString("entidad"),
            rs.getString("id_registro"),
            DateUtils.parseDateTime(rs.getString("fecha_hora")),
            rs.getString("detalle")
        );
    }
}
