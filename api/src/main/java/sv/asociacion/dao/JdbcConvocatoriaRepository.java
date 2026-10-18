package sv.asociacion.dao;

import sv.asociacion.config.DBConnection;
import sv.asociacion.exception.TraductorSQLException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.sql.DataSource;

/**
 * Implementacion JDBC de {@link ConvocatoriaRepository} sobre el DataSource con pool.
 *
 * <h2>Seguridad ante concurrencia</h2>
 * <ul>
 *   <li><b>Una conexion por tarea.</b> Cada llamada a {@link #convocarLote} pide su
 *       propia conexion al pool y la devuelve al terminar (try-with-resources).
 *       Ninguna {@code Connection} se comparte entre hilos: JDBC no garantiza que
 *       sean seguras para uso concurrente.</li>
 *   <li><b>La base decide los duplicados.</b> La restriccion
 *       {@code uk_asistencia_reunion_miembro (id_reunion, id_miembro)} impide dos
 *       filas para el mismo miembro en la misma reunion, aunque dos hilos o dos
 *       coordinadores convoquen a la vez. {@code INSERT IGNORE} convierte ese choque
 *       en "0 filas afectadas" en lugar de un error, lo que hace la operacion
 *       idempotente. No se usa un {@code synchronized} en memoria: no protegeria
 *       frente a otra instancia de la API ni frente al escritorio.</li>
 *   <li><b>Solo miembros activos.</b> El {@code INSERT ... SELECT} filtra en la misma
 *       sentencia, de modo que un miembro desactivado entre la lectura y la escritura
 *       no queda convocado.</li>
 *   <li><b>Lote atomico.</b> Cada lote se confirma en su propia transaccion: o entran
 *       todos sus miembros o ninguno, y un lote fallido no deshace a los demas.</li>
 * </ul>
 *
 * <p>Nota sobre {@code INSERT IGNORE}: tambien degrada a advertencia una clave
 * foranea invalida. Por eso el servicio valida antes que la reunion exista y el
 * SELECT solo produce miembros existentes.</p>
 */
public class JdbcConvocatoriaRepository implements ConvocatoriaRepository {

    static final String SQL_ACTIVOS =
        "SELECT id_miembro FROM miembro WHERE estado = 'ACTIVO' ORDER BY id_miembro";

    static final String SQL_CONVOCAR =
        "INSERT IGNORE INTO asistencia (id_reunion, id_miembro, asistio, observacion) "
            + "SELECT ?, m.id_miembro, FALSE, NULL FROM miembro m WHERE m.id_miembro = ? AND m.estado = 'ACTIVO'";

    /** Fuente de conexiones; en produccion, el DataSource con pool. */
    @FunctionalInterface
    interface FuenteConexiones {
        Connection obtener() throws SQLException;
    }

    private final FuenteConexiones fuente;

    /** Recibe el DataSource directamente (pruebas o configuracion explicita). */
    public JdbcConvocatoriaRepository(DataSource dataSource) {
        this.fuente = Objects.requireNonNull(dataSource, "El DataSource es obligatorio.")::getConnection;
    }

    private JdbcConvocatoriaRepository(FuenteConexiones fuente) {
        this.fuente = fuente;
    }

    /**
     * Usa el DataSource con pool de la aplicacion. Se resuelve en cada solicitud (y
     * no al arrancar) para que la API pueda iniciar aunque la base aun no responda.
     */
    public static JdbcConvocatoriaRepository conPoolDeLaAplicacion() {
        return new JdbcConvocatoriaRepository(() -> DBConnection.getInstance().getDataSource().getConnection());
    }

    @Override
    public List<Integer> idsMiembrosActivos() {
        try (Connection conn = fuente.obtener();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(SQL_ACTIVOS)) {
            List<Integer> ids = new ArrayList<>();
            while (rs.next()) {
                ids.add(rs.getInt(1));
            }
            return ids;
        } catch (SQLException e) {
            throw TraductorSQLException.traducir("consultar los miembros activos", e);
        }
    }

    @Override
    public int convocarLote(int idReunion, List<Integer> idsMiembros) {
        if (idsMiembros.isEmpty()) return 0;
        try (Connection conn = fuente.obtener()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(SQL_CONVOCAR)) {
                for (Integer idMiembro : idsMiembros) {
                    ps.setInt(1, idReunion);
                    ps.setInt(2, idMiembro);
                    ps.addBatch();
                }
                int creados = contarCreados(ps.executeBatch());
                conn.commit();
                return creados;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw TraductorSQLException.traducir("convocar un lote de " + idsMiembros.size() + " miembros", e);
        }
    }

    /** INSERT IGNORE informa 1 si inserto la fila y 0 si ya existia o el miembro no esta activo. */
    static int contarCreados(int[] resultados) {
        int creados = 0;
        for (int r : resultados) {
            if (r > 0) creados += r;
        }
        return creados;
    }
}
