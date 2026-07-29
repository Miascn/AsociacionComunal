package sv.asociacion.backend.dao;

import sv.asociacion.backend.config.DBConnection;
import sv.asociacion.backend.entity.Proyecto;
import sv.asociacion.backend.util.DateUtils;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProyectoDAO implements DAO<Proyecto, Integer> {
    @Override
    public Optional<Proyecto> findById(Integer id) {
        String sql = "SELECT * FROM proyecto WHERE id_proyecto = ?";
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
    public List<Proyecto> findAll() {
        List<Proyecto> proyectos = new ArrayList<>();
        String sql = "SELECT * FROM proyecto";
        try (Connection conn = DBConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                proyectos.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return proyectos;
    }

    @Override
    public Proyecto save(Proyecto entity) {
        String sql = "INSERT INTO proyecto (creado_por, nombre, descripcion, presupuesto, fecha_creacion, estado) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, entity.getCreadoPor());
            ps.setString(2, entity.getNombre());
            ps.setString(3, entity.getDescripcion());
            ps.setBigDecimal(4, entity.getPresupuesto());
            ps.setString(5, DateUtils.formatDate(entity.getFechaCreacion()));
            ps.setString(6, entity.getEstado().name());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    entity.setIdProyecto(keys.getInt(1));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return entity;
    }

    @Override
    public Proyecto update(Proyecto entity) {
        String sql = "UPDATE proyecto SET creado_por = ?, nombre = ?, descripcion = ?, presupuesto = ?, fecha_creacion = ?, estado = ? WHERE id_proyecto = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, entity.getCreadoPor());
            ps.setString(2, entity.getNombre());
            ps.setString(3, entity.getDescripcion());
            ps.setBigDecimal(4, entity.getPresupuesto());
            ps.setString(5, DateUtils.formatDate(entity.getFechaCreacion()));
            ps.setString(6, entity.getEstado().name());
            ps.setInt(7, entity.getIdProyecto());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return entity;
    }

    @Override
    public boolean delete(Integer id) {
        String sql = "DELETE FROM proyecto WHERE id_proyecto = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public List<Proyecto> findByEstado(Proyecto.Estado estado) {
        List<Proyecto> list = new ArrayList<>();
        String sql = "SELECT * FROM proyecto WHERE estado = ?";
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

    private Proyecto mapResultSet(ResultSet rs) throws SQLException {
        return new Proyecto(
            rs.getInt("id_proyecto"),
            rs.getInt("creado_por"),
            rs.getString("nombre"),
            rs.getString("descripcion"),
            rs.getBigDecimal("presupuesto"),
            DateUtils.parseDate(rs.getString("fecha_creacion")),
            Proyecto.Estado.valueOf(rs.getString("estado"))
        );
    }
}
