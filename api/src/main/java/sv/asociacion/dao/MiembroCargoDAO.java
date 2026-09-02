package sv.asociacion.dao;

import sv.asociacion.config.DBConnection;
import sv.asociacion.domain.dto.AsignacionCargoResponse;
import sv.asociacion.domain.entity.MiembroCargo;
import sv.asociacion.util.DateUtils;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MiembroCargoDAO implements DAO<MiembroCargo, Integer> {

    @Override
    public Optional<MiembroCargo> findById(Integer id) {
        String sql = "SELECT * FROM miembro_cargo WHERE id_miembro_cargo = ?";
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
    public List<MiembroCargo> findAll() {
        List<MiembroCargo> list = new ArrayList<>();
        String sql = "SELECT * FROM miembro_cargo";
        try (Connection conn = DBConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    @Override
    public MiembroCargo save(MiembroCargo entity) {
        String sql = "INSERT INTO miembro_cargo (id_miembro, id_cargo, id_periodo, fecha_asignacion, fecha_fin, motivo_salida, estado) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, entity.getIdMiembro());
            ps.setInt(2, entity.getIdCargo());
            ps.setInt(3, entity.getIdPeriodo());
            ps.setString(4, DateUtils.formatDate(entity.getFechaAsignacion()));
            ps.setString(5, entity.getFechaFin() != null ? DateUtils.formatDate(entity.getFechaFin()) : null);
            ps.setString(6, entity.getMotivoSalida());
            ps.setString(7, entity.getEstado() != null ? entity.getEstado().name() : MiembroCargo.Estado.ACTIVO.name());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    entity.setIdMiembroCargo(keys.getInt(1));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return entity;
    }

    @Override
    public MiembroCargo update(MiembroCargo entity) {
        String sql = "UPDATE miembro_cargo SET id_miembro = ?, id_cargo = ?, id_periodo = ?, fecha_asignacion = ?, fecha_fin = ?, motivo_salida = ?, estado = ? WHERE id_miembro_cargo = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, entity.getIdMiembro());
            ps.setInt(2, entity.getIdCargo());
            ps.setInt(3, entity.getIdPeriodo());
            ps.setString(4, DateUtils.formatDate(entity.getFechaAsignacion()));
            ps.setString(5, entity.getFechaFin() != null ? DateUtils.formatDate(entity.getFechaFin()) : null);
            ps.setString(6, entity.getMotivoSalida());
            ps.setString(7, entity.getEstado() != null ? entity.getEstado().name() : MiembroCargo.Estado.ACTIVO.name());
            ps.setInt(8, entity.getIdMiembroCargo());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return entity;
    }

    @Override
    public boolean delete(Integer id) {
        String sql = "DELETE FROM miembro_cargo WHERE id_miembro_cargo = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public List<MiembroCargo> findByMiembro(Integer idMiembro) {
        List<MiembroCargo> list = new ArrayList<>();
        String sql = "SELECT * FROM miembro_cargo WHERE id_miembro = ?";
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

    public List<MiembroCargo> findByPeriodo(Integer idPeriodo) {
        List<MiembroCargo> list = new ArrayList<>();
        String sql = "SELECT * FROM miembro_cargo WHERE id_periodo = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idPeriodo);
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

    public int countByCargo(Integer idCargo) {
        String sql = "SELECT COUNT(*) FROM miembro_cargo WHERE id_cargo = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idCargo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public Optional<MiembroCargo> findActiveByCargoAndPeriodo(Integer idCargo, Integer idPeriodo, Integer excludeId) {
        StringBuilder sql = new StringBuilder(
            "SELECT * FROM miembro_cargo WHERE id_cargo = ? AND id_periodo = ? AND estado = 'ACTIVO' "
        );
        if (excludeId != null) {
            sql.append("AND id_miembro_cargo != ? ");
        }
        sql.append("LIMIT 1");

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            ps.setInt(1, idCargo);
            ps.setInt(2, idPeriodo);
            if (excludeId != null) {
                ps.setInt(3, excludeId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapResultSet(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public Optional<MiembroCargo> findActiveByMiembroAndPeriodo(Integer idMiembro, Integer idPeriodo, Integer excludeId) {
        StringBuilder sql = new StringBuilder(
            "SELECT * FROM miembro_cargo WHERE id_miembro = ? AND id_periodo = ? AND estado = 'ACTIVO' "
        );
        if (excludeId != null) {
            sql.append("AND id_miembro_cargo != ? ");
        }
        sql.append("LIMIT 1");

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            ps.setInt(1, idMiembro);
            ps.setInt(2, idPeriodo);
            if (excludeId != null) {
                ps.setInt(3, excludeId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapResultSet(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public boolean revocar(Integer id, LocalDate fechaFin, String motivo) {
        String sql = "UPDATE miembro_cargo SET fecha_fin = ?, motivo_salida = ?, estado = 'REVOCADO' WHERE id_miembro_cargo = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, DateUtils.formatDate(fechaFin));
            ps.setString(2, motivo != null ? motivo.trim() : null);
            ps.setInt(3, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void finalizarPorPeriodo(Integer idPeriodo) {
        String sql = "UPDATE miembro_cargo SET estado = 'FINALIZADO' WHERE id_periodo = ? AND estado = 'ACTIVO'";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idPeriodo);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Optional<AsignacionCargoResponse> findDetalladoById(Integer id) {
        String sql = """
            SELECT mc.id_miembro_cargo,
                   m.id_miembro, CONCAT(m.nombres, ' ', m.apellidos) AS nombre_miembro, m.dui, m.telefono,
                   c.id_cargo, c.nombre AS nombre_cargo, c.nivel_jerarquico,
                   p.id_periodo, p.nombre AS nombre_periodo, p.estado AS estado_periodo,
                   mc.fecha_asignacion, mc.fecha_fin, mc.motivo_salida, mc.estado
            FROM miembro_cargo mc
            INNER JOIN miembro m ON mc.id_miembro = m.id_miembro
            INNER JOIN cargo c ON mc.id_cargo = c.id_cargo
            INNER JOIN periodo_directiva p ON mc.id_periodo = p.id_periodo
            WHERE mc.id_miembro_cargo = ?
        """;
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapDetailed(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public List<AsignacionCargoResponse> findDetalladoFiltered(Integer idPeriodo, String estado, String busqueda) {
        List<AsignacionCargoResponse> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
            SELECT mc.id_miembro_cargo,
                   m.id_miembro, CONCAT(m.nombres, ' ', m.apellidos) AS nombre_miembro, m.dui, m.telefono,
                   c.id_cargo, c.nombre AS nombre_cargo, c.nivel_jerarquico,
                   p.id_periodo, p.nombre AS nombre_periodo, p.estado AS estado_periodo,
                   mc.fecha_asignacion, mc.fecha_fin, mc.motivo_salida, mc.estado
            FROM miembro_cargo mc
            INNER JOIN miembro m ON mc.id_miembro = m.id_miembro
            INNER JOIN cargo c ON mc.id_cargo = c.id_cargo
            INNER JOIN periodo_directiva p ON mc.id_periodo = p.id_periodo
            WHERE 1=1
        """);
        List<Object> params = new ArrayList<>();

        if (idPeriodo != null) {
            sql.append(" AND mc.id_periodo = ?");
            params.add(idPeriodo);
        }
        if (estado != null && !estado.isBlank() && !"TODOS".equalsIgnoreCase(estado)) {
            sql.append(" AND mc.estado = ?");
            params.add(estado.trim().toUpperCase());
        }
        if (busqueda != null && !busqueda.isBlank()) {
            sql.append(" AND (LOWER(m.nombres) LIKE ? OR LOWER(m.apellidos) LIKE ? OR LOWER(c.nombre) LIKE ? OR m.dui LIKE ?)");
            String q = "%" + busqueda.trim().toLowerCase() + "%";
            params.add(q);
            params.add(q);
            params.add(q);
            params.add(q);
        }

        sql.append(" ORDER BY c.nivel_jerarquico ASC, mc.id_miembro_cargo ASC");

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapDetailed(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    private AsignacionCargoResponse mapDetailed(ResultSet rs) throws SQLException {
        return new AsignacionCargoResponse(
            rs.getInt("id_miembro_cargo"),
            rs.getInt("id_miembro"),
            rs.getString("nombre_miembro"),
            rs.getString("dui"),
            rs.getString("telefono"),
            rs.getInt("id_cargo"),
            rs.getString("nombre_cargo"),
            rs.getInt("nivel_jerarquico"),
            rs.getInt("id_periodo"),
            rs.getString("nombre_periodo"),
            rs.getString("estado_periodo"),
            rs.getString("fecha_asignacion"),
            rs.getString("fecha_fin"),
            rs.getString("motivo_salida"),
            rs.getString("estado")
        );
    }

    private MiembroCargo mapResultSet(ResultSet rs) throws SQLException {
        String estadoStr = null;
        try {
            estadoStr = rs.getString("estado");
        } catch (SQLException ignored) {}

        MiembroCargo.Estado estado = MiembroCargo.Estado.ACTIVO;
        if (estadoStr != null) {
            try {
                estado = MiembroCargo.Estado.valueOf(estadoStr.toUpperCase());
            } catch (Exception ignored) {}
        }

        LocalDate fechaFin = null;
        try {
            fechaFin = DateUtils.parseDate(rs.getString("fecha_fin"));
        } catch (SQLException ignored) {}

        String motivo = null;
        try {
            motivo = rs.getString("motivo_salida");
        } catch (SQLException ignored) {}

        return new MiembroCargo(
            rs.getInt("id_miembro_cargo"),
            rs.getInt("id_miembro"),
            rs.getInt("id_cargo"),
            rs.getInt("id_periodo"),
            DateUtils.parseDate(rs.getString("fecha_asignacion")),
            fechaFin,
            motivo,
            estado
        );
    }
}
