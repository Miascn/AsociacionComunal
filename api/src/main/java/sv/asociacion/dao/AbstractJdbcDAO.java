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
import java.util.Optional;
import javax.sql.DataSource;

/**
 * Base de los DAO JDBC: concentra el ciclo que todos repetian a mano.
 *
 * <p>Cada metodo de un DAO JDBC hacia lo mismo: pedir una conexion, preparar la
 * sentencia, asignar parametros, ejecutar, recorrer el {@link ResultSet}, cerrar
 * los tres recursos y decidir que hacer con la {@link SQLException}. Aqui ese
 * ciclo se escribe una sola vez (patron metodo plantilla) y cada DAO concreto
 * aporta solo su SQL, sus parametros y un {@link RowMapper}.</p>
 *
 * <ul>
 *   <li>Los recursos se cierran siempre con try-with-resources; la conexion vuelve
 *       al pool aunque falle la consulta.</li>
 *   <li>Toda {@link SQLException} se traduce con {@link TraductorSQLException}: no
 *       se silencia ninguna.</li>
 *   <li>Los parametros se asignan con {@link PreparedStatement}: nunca se concatena
 *       un valor en el SQL.</li>
 * </ul>
 *
 * @param <T>  entidad que maneja el DAO
 * @param <ID> tipo de su identificador
 */
public abstract class AbstractJdbcDAO<T, ID> implements DAO<T, ID> {

    /** De donde salen las conexiones: el pool global o un DataSource inyectado. */
    @FunctionalInterface
    protected interface ProveedorConexion {
        Connection obtener() throws SQLException;
    }

    private final ProveedorConexion proveedor;

    /** Usa el pool global de la aplicacion. */
    protected AbstractJdbcDAO() {
        this(() -> DBConnection.getInstance().getConnection());
    }

    /** Usa el DataSource indicado (por ejemplo, uno de prueba). */
    protected AbstractJdbcDAO(DataSource dataSource) {
        this(Objects.requireNonNull(dataSource, "El DataSource es obligatorio.")::getConnection);
    }

    private AbstractJdbcDAO(ProveedorConexion proveedor) {
        this.proveedor = proveedor;
    }

    /** Como se convierte una fila en la entidad de este DAO. */
    protected abstract RowMapper<T> mapeador();

    /** Nombre de la entidad para los mensajes de error, por ejemplo "asistencia". */
    protected abstract String entidad();

    protected Connection conexion() throws SQLException {
        return proveedor.obtener();
    }

    protected Optional<T> consultarUno(String sql, Object... parametros) {
        List<T> filas = consultarLista(sql, mapeador(), parametros);
        return filas.isEmpty() ? Optional.empty() : Optional.of(filas.get(0));
    }

    protected List<T> consultarLista(String sql, Object... parametros) {
        return consultarLista(sql, mapeador(), parametros);
    }

    /** Metodo generico: permite leer filas de otro tipo (por ejemplo, solo ids) con su propio mapeador. */
    protected <R> List<R> consultarLista(String sql, RowMapper<R> mapper, Object... parametros) {
        try (Connection conn = conexion();
             PreparedStatement ps = preparar(conn, sql, parametros);
             ResultSet rs = ps.executeQuery()) {
            List<R> resultado = new ArrayList<>();
            while (rs.next()) {
                resultado.add(mapper.mapear(rs));
            }
            return resultado;
        } catch (SQLException e) {
            throw TraductorSQLException.traducir("consultar datos de " + entidad(), e);
        }
    }

    /** Para COUNT(*) y consultas que devuelven un unico numero entero. */
    protected int consultarEntero(String sql, Object... parametros) {
        List<Integer> valores = consultarLista(sql, rs -> rs.getInt(1), parametros);
        return valores.isEmpty() ? 0 : valores.get(0);
    }

    protected boolean existe(String sql, Object... parametros) {
        return !consultarLista(sql, rs -> Boolean.TRUE, parametros).isEmpty();
    }

    /** INSERT, UPDATE o DELETE; devuelve las filas afectadas. */
    protected int actualizar(String sql, Object... parametros) {
        try (Connection conn = conexion();
             PreparedStatement ps = preparar(conn, sql, parametros)) {
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw TraductorSQLException.traducir("modificar datos de " + entidad(), e);
        }
    }

    /** INSERT que devuelve la clave autogenerada, o {@code null} si el motor no la informa. */
    protected Long insertarConClave(String sql, Object... parametros) {
        try (Connection conn = conexion();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            asignar(ps, parametros);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getLong(1) : null;
            }
        } catch (SQLException e) {
            throw TraductorSQLException.traducir("guardar datos de " + entidad(), e);
        }
    }

    private static PreparedStatement preparar(Connection conn, String sql, Object... parametros) throws SQLException {
        PreparedStatement ps = conn.prepareStatement(sql);
        try {
            asignar(ps, parametros);
            return ps;
        } catch (SQLException e) {
            ps.close();
            throw e;
        }
    }

    private static void asignar(PreparedStatement ps, Object... parametros) throws SQLException {
        for (int i = 0; i < parametros.length; i++) {
            ps.setObject(i + 1, parametros[i]);
        }
    }
}
