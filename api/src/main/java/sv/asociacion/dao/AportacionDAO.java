package sv.asociacion.dao;

import sv.asociacion.config.DBConnection;
import sv.asociacion.domain.entity.Aportacion;
import sv.asociacion.util.DateUtils;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AportacionDAO implements DAO<Aportacion, Long> {

    public record AportacionDetail(
        Aportacion aportacion,
        String nombreMiembro,
        String duiMiembro,
        String nombreProyecto
    ) {}

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

    public Optional<AportacionDetail> findByIdWithDetails(Long id) {
        String sql = "SELECT a.*, " +
                     "CONCAT(m.nombres, ' ', m.apellidos) AS nombre_miembro, " +
                     "m.dui AS dui_miembro, " +
                     "p.nombre AS nombre_proyecto " +
                     "FROM aportacion a " +
                     "INNER JOIN miembro m ON a.id_miembro = m.id_miembro " +
                     "LEFT JOIN proyecto p ON a.id_proyecto = p.id_proyecto " +
                     "WHERE a.id_aportacion = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Aportacion a = mapResultSet(rs);
                    return Optional.of(new AportacionDetail(
                        a,
                        rs.getString("nombre_miembro"),
                        rs.getString("dui_miembro"),
                        rs.getString("nombre_proyecto")
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    @Override
    public List<Aportacion> findAll() {
        List<Aportacion> aportaciones = new ArrayList<>();
        String sql = "SELECT * FROM aportacion ORDER BY fecha_pago DESC, id_aportacion DESC";
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
        String sql = "INSERT INTO aportacion (id_miembro, id_proyecto, periodo_mes, monto, fecha_pago, metodo_pago, referencia, estado) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, entity.getIdMiembro());
            if (entity.getIdProyecto() != null) {
                ps.setInt(2, entity.getIdProyecto());
            } else {
                ps.setNull(2, Types.INTEGER);
            }
            ps.setString(3, entity.getPeriodoMes());
            ps.setBigDecimal(4, entity.getMonto());
            ps.setString(5, DateUtils.formatDate(entity.getFechaPago()));
            ps.setString(6, entity.getMetodoPago().name());
            ps.setString(7, entity.getReferencia());
            ps.setString(8, entity.getEstado() != null ? entity.getEstado().name() : Aportacion.Estado.REGISTRADA.name());
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
        String sql = "UPDATE aportacion SET id_miembro = ?, id_proyecto = ?, periodo_mes = ?, monto = ?, fecha_pago = ?, metodo_pago = ?, referencia = ?, estado = ? WHERE id_aportacion = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, entity.getIdMiembro());
            if (entity.getIdProyecto() != null) {
                ps.setInt(2, entity.getIdProyecto());
            } else {
                ps.setNull(2, Types.INTEGER);
            }
            ps.setString(3, entity.getPeriodoMes());
            ps.setBigDecimal(4, entity.getMonto());
            ps.setString(5, DateUtils.formatDate(entity.getFechaPago()));
            ps.setString(6, entity.getMetodoPago().name());
            ps.setString(7, entity.getReferencia());
            ps.setString(8, entity.getEstado().name());
            ps.setLong(9, entity.getIdAportacion());
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

    public boolean anular(Long id) {
        String sql = "UPDATE aportacion SET estado = 'ANULADA' WHERE id_aportacion = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean existsByMiembroAndPeriodo(Integer idMiembro, String periodoMes, Long excludeId) {
        return existsByMiembroAndPeriodo(idMiembro, null, periodoMes, excludeId);
    }

    public boolean existsByMiembroAndPeriodo(Integer idMiembro, Integer idProyecto, String periodoMes, Long excludeId) {
        StringBuilder sql = new StringBuilder(
            "SELECT COUNT(*) FROM aportacion WHERE id_miembro = ? AND periodo_mes = ? AND estado = 'REGISTRADA' "
        );
        if (idProyecto != null) {
            sql.append("AND id_proyecto = ? ");
        } else {
            sql.append("AND id_proyecto IS NULL ");
        }
        if (excludeId != null) {
            sql.append("AND id_aportacion != ? ");
        }
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            ps.setInt(1, idMiembro);
            ps.setString(2, periodoMes);
            int idx = 3;
            if (idProyecto != null) {
                ps.setInt(idx++, idProyecto);
            }
            if (excludeId != null) {
                ps.setLong(idx, excludeId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public List<AportacionDetail> findFiltered(Integer idMiembro, Integer idProyecto, String periodo,
                                              String desde, String hasta, String metodo, String estado,
                                              String busqueda, int offset, int limit) {
        List<AportacionDetail> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
            "SELECT a.*, " +
            "CONCAT(m.nombres, ' ', m.apellidos) AS nombre_miembro, " +
            "m.dui AS dui_miembro, " +
            "p.nombre AS nombre_proyecto " +
            "FROM aportacion a " +
            "INNER JOIN miembro m ON a.id_miembro = m.id_miembro " +
            "LEFT JOIN proyecto p ON a.id_proyecto = p.id_proyecto " +
            "WHERE 1=1 "
        );
        List<Object> params = new ArrayList<>();
        appendFilters(sql, params, idMiembro, idProyecto, periodo, desde, hasta, metodo, estado, busqueda);

        sql.append("ORDER BY a.fecha_pago DESC, a.id_aportacion DESC LIMIT ? OFFSET ?");
        params.add(limit <= 0 ? 50 : limit);
        params.add(Math.max(0, offset));

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Aportacion a = mapResultSet(rs);
                    list.add(new AportacionDetail(
                        a,
                        rs.getString("nombre_miembro"),
                        rs.getString("dui_miembro"),
                        rs.getString("nombre_proyecto")
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public int countFiltered(Integer idMiembro, Integer idProyecto, String periodo,
                             String desde, String hasta, String metodo, String estado, String busqueda) {
        StringBuilder sql = new StringBuilder(
            "SELECT COUNT(*) " +
            "FROM aportacion a " +
            "INNER JOIN miembro m ON a.id_miembro = m.id_miembro " +
            "LEFT JOIN proyecto p ON a.id_proyecto = p.id_proyecto " +
            "WHERE 1=1 "
        );
        List<Object> params = new ArrayList<>();
        appendFilters(sql, params, idMiembro, idProyecto, periodo, desde, hasta, metodo, estado, busqueda);

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

    public BigDecimal sumFiltered(Integer idMiembro, Integer idProyecto, String periodo,
                                  String desde, String hasta, String metodo, String estado, String busqueda) {
        StringBuilder sql = new StringBuilder(
            "SELECT COALESCE(SUM(a.monto), 0) " +
            "FROM aportacion a " +
            "INNER JOIN miembro m ON a.id_miembro = m.id_miembro " +
            "LEFT JOIN proyecto p ON a.id_proyecto = p.id_proyecto " +
            "WHERE 1=1 "
        );
        List<Object> params = new ArrayList<>();
        appendFilters(sql, params, idMiembro, idProyecto, periodo, desde, hasta, metodo, estado, busqueda);

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getBigDecimal(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return BigDecimal.ZERO;
    }

    public List<Aportacion> findByMiembro(Integer idMiembro) {
        List<Aportacion> list = new ArrayList<>();
        String sql = "SELECT * FROM aportacion WHERE id_miembro = ? ORDER BY fecha_pago DESC, id_aportacion DESC";
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

    private void appendFilters(StringBuilder sql, List<Object> params,
                               Integer idMiembro, Integer idProyecto, String periodo,
                               String desde, String hasta, String metodo, String estado, String busqueda) {
        if (idMiembro != null) {
            sql.append("AND a.id_miembro = ? ");
            params.add(idMiembro);
        }
        if (idProyecto != null) {
            sql.append("AND a.id_proyecto = ? ");
            params.add(idProyecto);
        }
        if (periodo != null && !periodo.isBlank()) {
            sql.append("AND a.periodo_mes = ? ");
            params.add(periodo.trim());
        }
        if (desde != null && !desde.isBlank()) {
            sql.append("AND a.fecha_pago >= ? ");
            params.add(desde.trim());
        }
        if (hasta != null && !hasta.isBlank()) {
            sql.append("AND a.fecha_pago <= ? ");
            params.add(hasta.trim());
        }
        if (metodo != null && !metodo.isBlank() && !"TODOS".equalsIgnoreCase(metodo)) {
            sql.append("AND a.metodo_pago = ? ");
            params.add(metodo.trim().toUpperCase());
        }
        if (estado != null && !estado.isBlank() && !"TODOS".equalsIgnoreCase(estado)) {
            sql.append("AND a.estado = ? ");
            params.add(estado.trim().toUpperCase());
        }
        if (busqueda != null && !busqueda.isBlank()) {
            sql.append("AND (LOWER(m.nombres) LIKE ? OR LOWER(m.apellidos) LIKE ? OR m.dui LIKE ? OR LOWER(a.referencia) LIKE ?) ");
            String like = "%" + busqueda.trim().toLowerCase() + "%";
            params.add(like);
            params.add(like);
            params.add(like);
            params.add(like);
        }
    }

    private Aportacion mapResultSet(ResultSet rs) throws SQLException {
        Integer idProyecto = rs.getInt("id_proyecto");
        if (rs.wasNull()) idProyecto = null;

        return new Aportacion(
            rs.getLong("id_aportacion"),
            rs.getInt("id_miembro"),
            idProyecto,
            rs.getString("periodo_mes"),
            rs.getBigDecimal("monto"),
            DateUtils.parseDate(rs.getString("fecha_pago")),
            Aportacion.MetodoPago.valueOf(rs.getString("metodo_pago")),
            rs.getString("referencia"),
            Aportacion.Estado.valueOf(rs.getString("estado"))
        );
    }
}
