package sv.asociacion.dao;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sv.asociacion.domain.entity.Asistencia;
import sv.asociacion.exception.ConexionBDException;
import sv.asociacion.exception.PersistenciaException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * AbstractJdbcDAO a traves de AsistenciaDAO, con JDBC simulado por Mockito.
 * Verifica lo que la clase base promete: parametros con PreparedStatement,
 * mapeo con RowMapper, cierre de recursos y traduccion de SQLException.
 */
@ExtendWith(MockitoExtension.class)
class AsistenciaDaoJdbcTest {

    @Mock private DataSource dataSource;
    @Mock private Connection connection;
    @Mock private PreparedStatement statement;
    @Mock private ResultSet resultSet;

    private AsistenciaDAO dao;

    @BeforeEach
    void setUp() {
        dao = new AsistenciaDAO(dataSource);
    }

    @Test
    @DisplayName("findByReunion asigna el parametro, mapea cada fila y cierra conexion, sentencia y resultado")
    void consultaMapeaYCierraRecursos() throws SQLException {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true, true, false);
        when(resultSet.getLong("id_asistencia")).thenReturn(1L, 2L);
        when(resultSet.getInt("id_reunion")).thenReturn(7, 7);
        when(resultSet.getInt("id_miembro")).thenReturn(10, 20);
        when(resultSet.getBoolean("asistio")).thenReturn(true, false);
        when(resultSet.getString("observacion")).thenReturn("Puntual", (String) null);

        List<Asistencia> lista = dao.findByReunion(7);

        assertEquals(2, lista.size());
        assertEquals(10, lista.get(0).getIdMiembro());
        assertTrue(lista.get(0).isAsistio());
        assertEquals(20, lista.get(1).getIdMiembro());
        verify(connection).prepareStatement("SELECT * FROM asistencia WHERE id_reunion = ?");
        verify(statement).setObject(1, 7);
        verify(resultSet).close();
        verify(statement).close();
        verify(connection).close();
    }

    @Test
    @DisplayName("countByReunion devuelve el COUNT y 0 cuando no hay fila")
    void cuenta() throws SQLException {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true, false, false);
        when(resultSet.getInt(1)).thenReturn(42);

        assertEquals(42, dao.countByReunion(3));
        assertEquals(0, dao.countByReunion(3));
    }

    @Test
    @DisplayName("save usa la clave autogenerada como identificador")
    void guardaConClaveGenerada() throws SQLException {
        ResultSet keys = mock(ResultSet.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS))).thenReturn(statement);
        when(statement.getGeneratedKeys()).thenReturn(keys);
        when(keys.next()).thenReturn(true);
        when(keys.getLong(1)).thenReturn(99L);

        Asistencia nueva = new Asistencia(null, 7, 10, false, null);
        dao.save(nueva);

        assertEquals(99L, nueva.getIdAsistencia());
        verify(statement).setObject(1, 7);
        verify(statement).setObject(2, 10);
        verify(statement).setObject(3, false);
        verify(statement).setObject(4, null);
        verify(statement).executeUpdate();
        verify(connection).close();
    }

    @Test
    @DisplayName("delete informa si se elimino alguna fila")
    void elimina() throws SQLException {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeUpdate()).thenReturn(1, 0);

        assertTrue(dao.delete(5L));
        assertFalse(dao.delete(5L));
    }

    @Test
    @DisplayName("Un error SQL se traduce a PersistenciaException y la conexion vuelve al pool igual")
    void errorSqlSeTraduceYSeCierraLaConexion() throws SQLException {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        SQLException tablaInexistente = new SQLException("Table 'asistencia' doesn't exist", "42S02", 1146);
        when(statement.executeQuery()).thenThrow(tablaInexistente);

        PersistenciaException e = assertThrows(PersistenciaException.class, () -> dao.findByReunion(7));

        assertSame(tablaInexistente, e.getCause());
        assertFalse(e.getMessage().contains("doesn't exist"), "el mensaje no expone el texto de MySQL");
        verify(statement).close();
        verify(connection).close();
    }

    @Test
    @DisplayName("Si no se puede obtener conexion del pool, el error es ConexionBDException")
    void sinConexionEsConexionBD() throws SQLException {
        when(dataSource.getConnection()).thenThrow(new SQLException("Communications link failure", "08S01"));

        assertThrows(ConexionBDException.class, () -> dao.countByReunion(1));
        verifyNoInteractions(connection);
    }

    @Test
    @DisplayName("El lote transaccional confirma una sola vez y restaura autoCommit")
    void loteTransaccional() throws SQLException {
        PreparedStatement select = mock(PreparedStatement.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(startsWithInsert())).thenReturn(statement);
        when(connection.prepareStatement("SELECT * FROM asistencia WHERE id_reunion = ?")).thenReturn(select);
        when(select.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        dao.saveLoteTransaccional(7, List.of(
            new Asistencia(null, 7, 10, true, null),
            new Asistencia(null, 7, 20, false, "Excusa")));

        verify(statement, times(2)).addBatch();
        verify(statement).executeBatch();
        verify(connection).setAutoCommit(false);
        verify(connection).commit();
        verify(connection, never()).rollback();
        verify(connection).setAutoCommit(true);
    }

    private static String startsWithInsert() {
        return org.mockito.ArgumentMatchers.startsWith("INSERT INTO asistencia");
    }
}
