package sv.asociacion.dao;

import sv.asociacion.config.DBConnection;
import sv.asociacion.domain.entity.Votacion;
import sv.asociacion.util.DateUtils;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class VotacionDAO implements DAO<Votacion, Integer> {

    @Override
    public Optional<Votacion> findById(Integer id) {
        String sql = "SELECT * FROM votacion WHERE id_votacion = ?";
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
    public List<Votacion> findAll() {
        return findFiltered(null, null, null);
    }

    public List<Votacion> findFiltered(Votacion.Estado estado, Integer idProyecto, String busqueda) {
        List<Votacion> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM votacion WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (estado != null) {
            sql.append("AND estado = ? ");
            params.add(estado.name());
        }
        if (idProyecto != null) {
            sql.append("AND id_proyecto = ? ");
            params.add(idProyecto);
        }
        if (busqueda != null && !busqueda.isBlank()) {
            sql.append("AND (LOWER(titulo) LIKE ? OR LOWER(descripcion) LIKE ?) ");
            String q = "%" + busqueda.trim().toLowerCase() + "%";
            params.add(q);
            params.add(q);
        }

        sql.append("ORDER BY fecha_inicio DESC, id_votacion DESC");

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
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

    public boolean updateEstado(Integer id, Votacion.Estado nuevoEstado) {
        String sql = "UPDATE votacion SET estado = ? WHERE id_votacion = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nuevoEstado.name());
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public Votacion save(Votacion entity) {
        String sql = "INSERT INTO votacion (id_proyecto, titulo, descripcion, fecha_inicio, fecha_fin, estado) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            if (entity.getIdProyecto() != null) {
                ps.setInt(1, entity.getIdProyecto());
            } else {
                ps.setNull(1, Types.INTEGER);
            }
            ps.setString(2, entity.getTitulo());
            ps.setString(3, entity.getDescripcion());
            ps.setString(4, DateUtils.formatDateTime(entity.getFechaInicio()));
            ps.setString(5, DateUtils.formatDateTime(entity.getFechaFin()));
            ps.setString(6, entity.getEstado() != null ? entity.getEstado().name() : Votacion.Estado.BORRADOR.name());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    entity.setIdVotacion(keys.getInt(1));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return entity;
    }

    @Override
    public Votacion update(Votacion entity) {
        String sql = "UPDATE votacion SET id_proyecto = ?, titulo = ?, descripcion = ?, fecha_inicio = ?, fecha_fin = ?, estado = ? WHERE id_votacion = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (entity.getIdProyecto() != null) {
                ps.setInt(1, entity.getIdProyecto());
            } else {
                ps.setNull(1, Types.INTEGER);
            }
            ps.setString(2, entity.getTitulo());
            ps.setString(3, entity.getDescripcion());
            ps.setString(4, DateUtils.formatDateTime(entity.getFechaInicio()));
            ps.setString(5, DateUtils.formatDateTime(entity.getFechaFin()));
            ps.setString(6, entity.getEstado() != null ? entity.getEstado().name() : Votacion.Estado.BORRADOR.name());
            ps.setInt(7, entity.getIdVotacion());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return entity;
    }

    @Override
    public boolean delete(Integer id) {
        String sql = "DELETE FROM votacion WHERE id_votacion = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public List<Votacion> findByProyecto(Integer idProyecto) {
        return findFiltered(null, idProyecto, null);
    }

    private Votacion mapResultSet(ResultSet rs) throws SQLException {
        int idProyectoVal = rs.getInt("id_proyecto");
        Integer idProyecto = rs.wasNull() ? null : idProyectoVal;

        String descripcion = null;
        try {
            descripcion = rs.getString("descripcion");
        } catch (SQLException ignored) {}

        String estadoStr = rs.getString("estado");
        Votacion.Estado estado = Votacion.Estado.BORRADOR;
        try {
            estado = Votacion.Estado.valueOf(estadoStr.toUpperCase());
        } catch (Exception ignored) {}

        return new Votacion(
            rs.getInt("id_votacion"),
            idProyecto,
            rs.getString("titulo"),
            descripcion,
            DateUtils.parseDateTime(rs.getString("fecha_inicio")),
            DateUtils.parseDateTime(rs.getString("fecha_fin")),
            estado
        );
    }
}
