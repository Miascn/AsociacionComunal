package sv.asociacion.dao;

import sv.asociacion.config.DBConnection;
import sv.asociacion.domain.entity.Bitacora;
import sv.asociacion.util.DateUtils;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class BitacoraDAO implements DAO<Bitacora, Long> {

    public record BitacoraEntry(Bitacora bitacora, String nombreUsuario) {}

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

    public Optional<BitacoraEntry> findByIdWithUser(Long id) {
        String sql = "SELECT b.*, u.nombre_usuario " +
                     "FROM bitacora b " +
                     "LEFT JOIN usuario u ON b.id_usuario = u.id_usuario " +
                     "WHERE b.id_bitacora = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Bitacora b = mapResultSet(rs);
                    String usuario = rs.getString("nombre_usuario");
                    return Optional.of(new BitacoraEntry(b, usuario != null ? usuario : "Usuario #" + b.getIdUsuario()));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    @Override
    public List<Bitacora> findAll() {
        List<Bitacora> bitacoras = new ArrayList<>();
        String sql = "SELECT * FROM bitacora ORDER BY fecha_hora DESC, id_bitacora DESC";
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
        throw new UnsupportedOperationException("La bitácora es inmutable y no permite modificaciones.");
    }

    @Override
    public boolean delete(Long id) {
        throw new UnsupportedOperationException("La bitácora es inmutable y no permite eliminaciones.");
    }

    public List<BitacoraEntry> findFiltered(String usuario, String entidad, String accion,
                                            String fechaDesde, String fechaHasta, int offset, int limit) {
        List<BitacoraEntry> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
            "SELECT b.*, u.nombre_usuario " +
            "FROM bitacora b " +
            "LEFT JOIN usuario u ON b.id_usuario = u.id_usuario " +
            "WHERE 1=1 "
        );
        List<Object> params = new ArrayList<>();
        appendFilters(sql, params, usuario, entidad, accion, fechaDesde, fechaHasta);

        sql.append("ORDER BY b.fecha_hora DESC, b.id_bitacora DESC LIMIT ? OFFSET ?");
        params.add(limit <= 0 ? 50 : limit);
        params.add(Math.max(0, offset));

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Bitacora b = mapResultSet(rs);
                    String u = rs.getString("nombre_usuario");
                    list.add(new BitacoraEntry(b, u != null ? u : "Usuario #" + b.getIdUsuario()));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public int countFiltered(String usuario, String entidad, String accion, String fechaDesde, String fechaHasta) {
        StringBuilder sql = new StringBuilder(
            "SELECT COUNT(*) " +
            "FROM bitacora b " +
            "LEFT JOIN usuario u ON b.id_usuario = u.id_usuario " +
            "WHERE 1=1 "
        );
        List<Object> params = new ArrayList<>();
        appendFilters(sql, params, usuario, entidad, accion, fechaDesde, fechaHasta);

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public List<Bitacora> findByUsuario(Integer idUsuario) {
        List<Bitacora> list = new ArrayList<>();
        String sql = "SELECT * FROM bitacora WHERE id_usuario = ? ORDER BY fecha_hora DESC, id_bitacora DESC";
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
        String sql = "SELECT * FROM bitacora WHERE entidad = ? ORDER BY fecha_hora DESC, id_bitacora DESC";
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

    private void appendFilters(StringBuilder sql, List<Object> params,
                               String usuario, String entidad, String accion,
                               String fechaDesde, String fechaHasta) {
        if (usuario != null && !usuario.isBlank()) {
            sql.append("AND (LOWER(u.nombre_usuario) LIKE ? OR CAST(b.id_usuario AS CHAR) = ?) ");
            String like = "%" + usuario.trim().toLowerCase() + "%";
            params.add(like);
            params.add(usuario.trim());
        }
        if (entidad != null && !entidad.isBlank() && !"TODAS".equalsIgnoreCase(entidad.trim())) {
            sql.append("AND LOWER(b.entidad) = LOWER(?) ");
            params.add(entidad.trim());
        }
        if (accion != null && !accion.isBlank() && !"TODAS".equalsIgnoreCase(accion.trim())) {
            sql.append("AND LOWER(b.accion) = LOWER(?) ");
            params.add(accion.trim());
        }
        if (fechaDesde != null && !fechaDesde.isBlank()) {
            sql.append("AND b.fecha_hora >= ? ");
            params.add(fechaDesde.trim().length() == 10 ? fechaDesde.trim() + " 00:00:00" : fechaDesde.trim());
        }
        if (fechaHasta != null && !fechaHasta.isBlank()) {
            sql.append("AND b.fecha_hora <= ? ");
            params.add(fechaHasta.trim().length() == 10 ? fechaHasta.trim() + " 23:59:59" : fechaHasta.trim());
        }
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
