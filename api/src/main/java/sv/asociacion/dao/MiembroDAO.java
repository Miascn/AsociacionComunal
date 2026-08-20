package sv.asociacion.dao;

import sv.asociacion.config.DBConnection;
import sv.asociacion.domain.entity.Miembro;
import sv.asociacion.util.DateUtils;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MiembroDAO implements DAO<Miembro, Integer> {
    @Override
    public Optional<Miembro> findById(Integer id) {
        String sql = "SELECT * FROM miembro WHERE id_miembro = ?";
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
    public List<Miembro> findAll() {
        List<Miembro> miembros = new ArrayList<>();
        String sql = "SELECT * FROM miembro";
        try (Connection conn = DBConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                miembros.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return miembros;
    }

    @Override
    public Miembro save(Miembro entity) {
        String sql = "INSERT INTO miembro (dui, tipo_documento, pais_origen, id_vivienda, nombres, apellidos, telefono, correo, direccion, fecha_ingreso, estado) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, entity.getDui());
            ps.setString(2, entity.getTipoDocumento());
            ps.setString(3, entity.getPaisOrigen());
            if (entity.getIdVivienda() == null) ps.setNull(4, Types.INTEGER); else ps.setInt(4, entity.getIdVivienda());
            ps.setString(5, entity.getNombres());
            ps.setString(6, entity.getApellidos());
            ps.setString(7, entity.getTelefono());
            ps.setString(8, entity.getCorreo());
            ps.setString(9, entity.getDireccion());
            ps.setString(10, DateUtils.formatDate(entity.getFechaIngreso()));
            ps.setString(11, entity.getEstado().name());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    entity.setIdMiembro(keys.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("No fue posible guardar el miembro.", e);
        }
        return entity;
    }

    public boolean existsByDui(String dui) {
        String sql = "SELECT 1 FROM miembro WHERE dui = ? LIMIT 1";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, dui);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("No fue posible validar el DUI.", e);
        }
    }

    @Override
    public Miembro update(Miembro entity) {
        String sql = "UPDATE miembro SET dui = ?, tipo_documento = ?, pais_origen = ?, id_vivienda = ?, nombres = ?, apellidos = ?, telefono = ?, correo = ?, direccion = ?, fecha_ingreso = ?, estado = ? WHERE id_miembro = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, entity.getDui());
            ps.setString(2, entity.getTipoDocumento());
            ps.setString(3, entity.getPaisOrigen());
            if (entity.getIdVivienda() == null) ps.setNull(4, Types.INTEGER); else ps.setInt(4, entity.getIdVivienda());
            ps.setString(5, entity.getNombres());
            ps.setString(6, entity.getApellidos());
            ps.setString(7, entity.getTelefono());
            ps.setString(8, entity.getCorreo());
            ps.setString(9, entity.getDireccion());
            ps.setString(10, DateUtils.formatDate(entity.getFechaIngreso()));
            ps.setString(11, entity.getEstado().name());
            ps.setInt(12, entity.getIdMiembro());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return entity;
    }

    @Override
    public boolean delete(Integer id) {
        String sql = "DELETE FROM miembro WHERE id_miembro = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public List<Miembro> findByEstado(Miembro.Estado estado) {
        List<Miembro> miembros = new ArrayList<>();
        String sql = "SELECT * FROM miembro WHERE estado = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, estado.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    miembros.add(mapResultSet(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return miembros;
    }

    private Miembro mapResultSet(ResultSet rs) throws SQLException {
        return new Miembro(
            rs.getInt("id_miembro"),
            rs.getString("dui"),
            rs.getString("tipo_documento"),
            rs.getString("pais_origen"),
            (Integer) rs.getObject("id_vivienda"),
            rs.getString("nombres"),
            rs.getString("apellidos"),
            rs.getString("telefono"),
            rs.getString("correo"),
            rs.getString("direccion"),
            DateUtils.parseDate(rs.getString("fecha_ingreso")),
            Miembro.Estado.valueOf(rs.getString("estado"))
        );
    }
}
