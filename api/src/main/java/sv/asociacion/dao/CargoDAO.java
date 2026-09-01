package sv.asociacion.dao;

import sv.asociacion.config.DBConnection;
import sv.asociacion.domain.entity.Cargo;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CargoDAO implements DAO<Cargo, Integer> {

    public record CargoDetail(Cargo cargo, int totalAsignaciones) {}

    @Override
    public Optional<Cargo> findById(Integer id) {
        String sql = "SELECT * FROM cargo WHERE id_cargo = ?";
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

    public Optional<CargoDetail> findByIdWithDetail(Integer id) {
        String sql = "SELECT c.*, COUNT(mc.id_miembro_cargo) AS total_asignaciones " +
                     "FROM cargo c " +
                     "LEFT JOIN miembro_cargo mc ON c.id_cargo = mc.id_cargo " +
                     "WHERE c.id_cargo = ? " +
                     "GROUP BY c.id_cargo";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Cargo cargo = mapResultSet(rs);
                    return Optional.of(new CargoDetail(cargo, rs.getInt("total_asignaciones")));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public Optional<Cargo> findByNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) return Optional.empty();
        String sql = "SELECT * FROM cargo WHERE LOWER(nombre) = LOWER(?)";
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
    public List<Cargo> findAll() {
        List<Cargo> cargos = new ArrayList<>();
        String sql = "SELECT * FROM cargo ORDER BY nivel_jerarquico ASC, id_cargo ASC";
        try (Connection conn = DBConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                cargos.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return cargos;
    }

    public List<CargoDetail> findFiltered(Boolean activo, String busqueda) {
        List<CargoDetail> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
            "SELECT c.*, COUNT(mc.id_miembro_cargo) AS total_asignaciones " +
            "FROM cargo c " +
            "LEFT JOIN miembro_cargo mc ON c.id_cargo = mc.id_cargo " +
            "WHERE 1=1 "
        );
        List<Object> params = new ArrayList<>();

        if (activo != null) {
            sql.append("AND c.activo = ? ");
            params.add(activo);
        }
        if (busqueda != null && !busqueda.isBlank()) {
            sql.append("AND (LOWER(c.nombre) LIKE ? OR LOWER(c.descripcion) LIKE ?) ");
            String like = "%" + busqueda.trim().toLowerCase() + "%";
            params.add(like);
            params.add(like);
        }

        sql.append("GROUP BY c.id_cargo, c.nombre, c.descripcion, c.nivel_jerarquico, c.activo ");
        sql.append("ORDER BY c.nivel_jerarquico ASC, c.id_cargo ASC");

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Cargo cargo = mapResultSet(rs);
                    list.add(new CargoDetail(cargo, rs.getInt("total_asignaciones")));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    @Override
    public Cargo save(Cargo entity) {
        String sql = "INSERT INTO cargo (nombre, descripcion, nivel_jerarquico, activo) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, entity.getNombre());
            ps.setString(2, entity.getDescripcion());
            ps.setInt(3, entity.getNivelJerarquico() != null ? entity.getNivelJerarquico() : 1);
            ps.setBoolean(4, entity.getActivo() != null ? entity.getActivo() : true);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    entity.setIdCargo(keys.getInt(1));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return entity;
    }

    @Override
    public Cargo update(Cargo entity) {
        String sql = "UPDATE cargo SET nombre = ?, descripcion = ?, nivel_jerarquico = ?, activo = ? WHERE id_cargo = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, entity.getNombre());
            ps.setString(2, entity.getDescripcion());
            ps.setInt(3, entity.getNivelJerarquico() != null ? entity.getNivelJerarquico() : 1);
            ps.setBoolean(4, entity.getActivo() != null ? entity.getActivo() : true);
            ps.setInt(5, entity.getIdCargo());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return entity;
    }

    public boolean toggleActivo(Integer id, boolean activo) {
        String sql = "UPDATE cargo SET activo = ? WHERE id_cargo = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, activo);
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean delete(Integer id) {
        String sql = "DELETE FROM cargo WHERE id_cargo = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private Cargo mapResultSet(ResultSet rs) throws SQLException {
        Integer nivel = null;
        try {
            nivel = rs.getInt("nivel_jerarquico");
            if (rs.wasNull()) nivel = 1;
        } catch (SQLException ignored) {
            nivel = 1;
        }

        Boolean activo = true;
        try {
            activo = rs.getBoolean("activo");
            if (rs.wasNull()) activo = true;
        } catch (SQLException ignored) {
            activo = true;
        }

        return new Cargo(
            rs.getInt("id_cargo"),
            rs.getString("nombre"),
            rs.getString("descripcion"),
            nivel,
            activo
        );
    }
}
