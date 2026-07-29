package sv.asociacion.backend.dao;

import sv.asociacion.backend.config.DBConnection;
import sv.asociacion.backend.entity.Aportacion;
import sv.asociacion.backend.util.DateUtils;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AportacionDAO implements DAO<Aportacion, Long> {
    @Override
    public Optional<Aportacion> findById(Long id) {
        String sql = "SELECT * FROM aportacion WHERE id_aportacion = ?";
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
    public List<Aportacion> findAll() {
        List<Aportacion> aportaciones = new ArrayList<>();
        String sql = "SELECT * FROM aportacion";
        try (Connection conn = DBConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                aportaciones.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return aportaciones;
    }

    @Override
    public Aportacion save(Aportacion entity) {
        String sql = "INSERT INTO aportacion (id_miembro, periodo_mes, monto, fecha_pago, metodo_pago, referencia, estado) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, entity.getIdMiembro());
            ps.setString(2, entity.getPeriodoMes());
            ps.setBigDecimal(3, entity.getMonto());
            ps.setString(4, DateUtils.formatDate(entity.getFechaPago()));
            ps.setString(5, entity.getMetodoPago().name());
            ps.setString(6, entity.getReferencia());
            ps.setString(7, entity.getEstado().name());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    entity.setIdAportacion(keys.getLong(1));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return entity;
    }

    @Override
    public Aportacion update(Aportacion entity) {
        String sql = "UPDATE aportacion SET id_miembro = ?, periodo_mes = ?, monto = ?, fecha_pago = ?, metodo_pago = ?, referencia = ?, estado = ? WHERE id_aportacion = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, entity.getIdMiembro());
            ps.setString(2, entity.getPeriodoMes());
            ps.setBigDecimal(3, entity.getMonto());
            ps.setString(4, DateUtils.formatDate(entity.getFechaPago()));
            ps.setString(5, entity.getMetodoPago().name());
            ps.setString(6, entity.getReferencia());
            ps.setString(7, entity.getEstado().name());
            ps.setLong(8, entity.getIdAportacion());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return entity;
    }

    @Override
    public boolean delete(Long id) {
        String sql = "DELETE FROM aportacion WHERE id_aportacion = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public List<Aportacion> findByMiembro(Integer idMiembro) {
        List<Aportacion> list = new ArrayList<>();
        String sql = "SELECT * FROM aportacion WHERE id_miembro = ?";
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

    private Aportacion mapResultSet(ResultSet rs) throws SQLException {
        return new Aportacion(
            rs.getLong("id_aportacion"),
            rs.getInt("id_miembro"),
            rs.getString("periodo_mes"),
            rs.getBigDecimal("monto"),
            DateUtils.parseDate(rs.getString("fecha_pago")),
            Aportacion.MetodoPago.valueOf(rs.getString("metodo_pago")),
            rs.getString("referencia"),
            Aportacion.Estado.valueOf(rs.getString("estado"))
        );
    }
}
