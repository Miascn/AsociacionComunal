package sv.asociacion.exception;

import org.apache.tomcat.jdbc.pool.PoolExhaustedException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.SQLNonTransientConnectionException;
import java.sql.SQLSyntaxErrorException;

import static org.junit.jupiter.api.Assertions.*;

class TraductorSQLExceptionTest {

    @Test
    @DisplayName("Communications link failure (SQLState 08S01) se traduce a ConexionBDException")
    void sqlState08EsFalloDeConexion() {
        SQLException caida = new SQLException("Communications link failure", "08S01", 0);

        PersistenciaException traducida = TraductorSQLException.traducir("consultar las reuniones", caida);

        assertInstanceOf(ConexionBDException.class, traducida);
        assertSame(caida, traducida.getCause(), "la SQLException original se conserva como causa");
    }

    @Test
    @DisplayName("Pool agotado y conexiones no transitorias tambien son fallos de conexion")
    void poolAgotadoYConexionNoTransitoria() {
        assertTrue(TraductorSQLException.esFalloDeConexion(new PoolExhaustedException("Timeout: Pool empty")));
        assertTrue(TraductorSQLException.esFalloDeConexion(new SQLNonTransientConnectionException("Access denied")));
    }

    @Test
    @DisplayName("La causa de conexion se detecta aunque venga envuelta en otra SQLException")
    void detectaCausaAnidada() {
        SQLException envoltura = new SQLException("fallo al preparar", "HY000");
        envoltura.initCause(new SQLException("socket cerrado", "08003"));
        assertTrue(TraductorSQLException.esFalloDeConexion(envoltura));
    }

    @Test
    @DisplayName("Errores de sintaxis o integridad no son caida de la base: PersistenciaException generica")
    void otrosErroresSonPersistenciaGenerica() {
        PersistenciaException sintaxis = TraductorSQLException.traducir("consultar",
            new SQLSyntaxErrorException("Unknown column 'x' in 'field list'", "42S22", 1054));
        PersistenciaException integridad = TraductorSQLException.traducir("guardar",
            new SQLIntegrityConstraintViolationException("Duplicate entry", "23000", 1062));

        assertEquals(PersistenciaException.class, sintaxis.getClass());
        assertEquals(PersistenciaException.class, integridad.getClass());
    }

    @Test
    @DisplayName("El mensaje de la excepcion traducida no contiene el texto del servidor de base de datos")
    void elMensajeNoFiltraSql() {
        PersistenciaException e = TraductorSQLException.traducir("consultar los miembros",
            new SQLSyntaxErrorException("Table 'asociacion_comunal.miembro' doesn't exist", "42S02", 1146));

        assertEquals("No fue posible consultar los miembros.", e.getMessage());
        assertFalse(e.getMessage().contains("asociacion_comunal"));
    }

    @Test
    @DisplayName("Jerarquia: los errores de negocio comparten base y VotoDuplicado es un conflicto")
    void jerarquia() {
        assertInstanceOf(NegocioException.class, new ValidacionException("x"));
        assertInstanceOf(NegocioException.class, new RecursoNoEncontradoException("x"));
        assertInstanceOf(ConflictoException.class, new VotoDuplicadoException("x", null));
        assertInstanceOf(PersistenciaException.class, new ConexionBDException("x", null));
        assertTrue(RuntimeException.class.isAssignableFrom(NegocioException.class),
            "no comprobada: no obliga a declarar throws en cada capa intermedia");
    }
}
