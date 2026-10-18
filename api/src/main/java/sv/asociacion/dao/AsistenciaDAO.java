package sv.asociacion.dao;

import sv.asociacion.domain.entity.Asistencia;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;

/**
 * Acceso a la tabla {@code asistencia}.
 *
 * <p>Avance 3: hereda de {@link AbstractJdbcDAO}. Cada metodo declara solo su SQL y
 * sus parametros; la gestion de conexiones, sentencias, resultados y errores es la
 * de la clase base. El archivo paso de 250 a unas 150 lineas sin cambiar su API publica.</p>
 */
public class AsistenciaDAO extends AbstractJdbcDAO<Asistencia, Long> {

    private static final String SELECT = "SELECT * FROM asistencia";

    public AsistenciaDAO() {
        super();
    }

    public AsistenciaDAO(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    protected RowMapper<Asistencia> mapeador() {
        return AsistenciaDAO::mapear;
    }

    @Override
    protected String entidad() {
        return "asistencia";
    }

    @Override
    public Optional<Asistencia> findById(Long id) {
        return consultarUno(SELECT + " WHERE id_asistencia = ?", id);
    }

    @Override
    public List<Asistencia> findAll() {
        return consultarLista(SELECT);
    }

    @Override
    public Asistencia save(Asistencia entity) {
        Long id = insertarConClave(
            "INSERT INTO asistencia (id_reunion, id_miembro, asistio, observacion) VALUES (?, ?, ?, ?)",
            entity.getIdReunion(), entity.getIdMiembro(), entity.isAsistio(), entity.getObservacion());
        if (id != null) entity.setIdAsistencia(id);
        return entity;
    }

    @Override
    public Asistencia update(Asistencia entity) {
        actualizar("UPDATE asistencia SET id_reunion = ?, id_miembro = ?, asistio = ?, observacion = ? WHERE id_asistencia = ?",
            entity.getIdReunion(), entity.getIdMiembro(), entity.isAsistio(), entity.getObservacion(), entity.getIdAsistencia());
        return entity;
    }

    @Override
    public boolean delete(Long id) {
        return actualizar("DELETE FROM asistencia WHERE id_asistencia = ?", id) > 0;
    }

    public List<Asistencia> findByReunion(Integer idReunion) {
        return consultarLista(SELECT + " WHERE id_reunion = ?", idReunion);
    }

    public List<Asistencia> findByMiembro(Integer idMiembro) {
        return consultarLista(SELECT + " WHERE id_miembro = ?", idMiembro);
    }

    public int countByReunion(Integer idReunion) {
        return consultarEntero("SELECT COUNT(*) FROM asistencia WHERE id_reunion = ?", idReunion);
    }

    public int countAsistieronByReunion(Integer idReunion) {
        return consultarEntero("SELECT COUNT(*) FROM asistencia WHERE id_reunion = ? AND asistio = 1", idReunion);
    }

    public boolean existsByReunionAndMiembro(Integer idReunion, Integer idMiembro) {
        return existe("SELECT 1 FROM asistencia WHERE id_reunion = ? AND id_miembro = ?", idReunion, idMiembro);
    }

    public Optional<Asistencia> findByReunionAndMiembro(Integer idReunion, Integer idMiembro) {
        return consultarUno(SELECT + " WHERE id_reunion = ? AND id_miembro = ?", idReunion, idMiembro);
    }

    public boolean toggleAsistencia(Long idAsistencia, boolean asistio, String observacion) {
        return actualizar("UPDATE asistencia SET asistio = ?, observacion = ? WHERE id_asistencia = ?",
            asistio, observacion, idAsistencia) > 0;
    }

    public int deleteByReunion(Integer idReunion) {
        return actualizar("DELETE FROM asistencia WHERE id_reunion = ?", idReunion);
    }

    /**
     * Registra o actualiza un lote completo de asistencias en una sola transaccion:
     * o se guardan todas o ninguna (politica de lote atomico del proyecto).
     */
    public List<Asistencia> saveLoteTransaccional(Integer idReunion, List<Asistencia> asistencias) throws SQLException {
        String sql = "INSERT INTO asistencia (id_reunion, id_miembro, asistio, observacion) VALUES (?, ?, ?, ?) " +
                     "ON DUPLICATE KEY UPDATE asistio = VALUES(asistio), observacion = VALUES(observacion)";
        try (Connection conn = conexion()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (Asistencia a : asistencias) {
                    ps.setInt(1, idReunion);
                    ps.setInt(2, a.getIdMiembro());
                    ps.setBoolean(3, a.isAsistio());
                    ps.setString(4, a.getObservacion());
                    ps.addBatch();
                }
                ps.executeBatch();
                conn.commit();
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
        return findByReunion(idReunion);
    }

    static Asistencia mapear(ResultSet rs) throws SQLException {
        return new Asistencia(
            rs.getLong("id_asistencia"),
            rs.getInt("id_reunion"),
            rs.getInt("id_miembro"),
            rs.getBoolean("asistio"),
            rs.getString("observacion")
        );
    }
}
