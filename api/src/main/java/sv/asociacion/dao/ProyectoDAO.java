package sv.asociacion.dao;

import sv.asociacion.config.DBConnection;
import sv.asociacion.domain.entity.Proyecto;
import sv.asociacion.util.DateUtils;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProyectoDAO implements DAO<Proyecto, Integer> {

    public record ProyectoDetail(Proyecto proyecto, String nombreCreador) {}

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

    public Optional<ProyectoDetail> findByIdWithCreator(Integer id) {
        String sql = "SELECT p.*, u.nombre_usuario AS nombre_creador " +
                     "FROM proyecto p " +
                     "LEFT JOIN usuario u ON p.creado_por = u.id_usuario " +
                     "WHERE p.id_proyecto = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Proyecto p = mapResultSet(rs);
                    return Optional.of(new ProyectoDetail(p, rs.getString("nombre_creador")));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public Optional<Proyecto> findByNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) return Optional.empty();
        String sql = "SELECT * FROM proyecto WHERE LOWER(nombre) = LOWER(?)";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nombre.trim());
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
        String sql = "SELECT * FROM proyecto ORDER BY id_proyecto DESC";
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

    public List<ProyectoDetail> findFiltered(Proyecto.Estado estado, Integer creadoPor, String busqueda) {
        List<ProyectoDetail> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
            "SELECT p.*, u.nombre_usuario AS nombre_creador " +
            "FROM proyecto p " +
            "LEFT JOIN usuario u ON p.creado_por = u.id_usuario " +
            "WHERE 1=1 "
        );
        List<Object> params = new ArrayList<>();

        if (estado != null) {
            sql.append("AND p.estado = ? ");
            params.add(estado.name());
        }
        if (creadoPor != null) {
            sql.append("AND p.creado_por = ? ");
            params.add(creadoPor);
        }
        if (busqueda != null && !busqueda.isBlank()) {
            sql.append("AND (LOWER(p.nombre) LIKE ? OR LOWER(p.descripcion) LIKE ?) ");
            String like = "%" + busqueda.trim().toLowerCase() + "%";
            params.add(like);
            params.add(like);
        }

        sql.append("ORDER BY p.id_proyecto DESC");

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Proyecto p = mapResultSet(rs);
                    list.add(new ProyectoDetail(p, rs.getString("nombre_creador")));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
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

    public boolean updateEstado(Integer id, Proyecto.Estado estado) {
        String sql = "UPDATE proyecto SET estado = ? WHERE id_proyecto = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, estado.name());
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
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
        String sql = "SELECT * FROM proyecto WHERE estado = ? ORDER BY id_proyecto DESC";
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
