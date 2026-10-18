package sv.asociacion.exception;

import org.apache.tomcat.jdbc.pool.PoolExhaustedException;

import java.sql.SQLException;
import java.sql.SQLNonTransientConnectionException;
import java.sql.SQLTransientConnectionException;

/**
 * Traduce una {@link SQLException} de JDBC a la excepcion de dominio adecuada.
 *
 * <p>Concentra en un solo lugar la decision "esto es la base caida o un error de
 * la operacion", que antes no se tomaba en ninguna parte: cada DAO imprimia la
 * traza y seguia. Los DAO llaman a {@link #traducir(String, SQLException)} desde
 * su bloque catch y lanzan el resultado.</p>
 */
public final class TraductorSQLException {

    /** Clase SQLState 08: excepciones de conexion (estandar SQL / X/Open). */
    private static final String SQLSTATE_CONEXION = "08";

    private TraductorSQLException() {
    }

    /**
     * @param operacion descripcion segura para el cliente, por ejemplo
     *                  "consultar las reuniones"; nunca debe contener datos del SQL
     * @param causa     la excepcion JDBC original, que se conserva como causa
     */
    public static PersistenciaException traducir(String operacion, SQLException causa) {
        if (esFalloDeConexion(causa)) {
            return new ConexionBDException(
                "La base de datos no está disponible en este momento. No fue posible " + operacion + ".", causa);
        }
        return new PersistenciaException("No fue posible " + operacion + ".", causa);
    }

    /**
     * Una conexion fallida se reconoce por el tipo (pool agotado, conexion
     * transitoria o permanente) o por el SQLState 08xxx que usa MySQL Connector/J
     * para "Communications link failure" y credenciales invalidas en el handshake.
     */
    public static boolean esFalloDeConexion(SQLException e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof PoolExhaustedException
                || t instanceof SQLTransientConnectionException
                || t instanceof SQLNonTransientConnectionException) {
                return true;
            }
            if (t instanceof SQLException sql
                && sql.getSQLState() != null
                && sql.getSQLState().startsWith(SQLSTATE_CONEXION)) {
                return true;
            }
        }
        return false;
    }
}
