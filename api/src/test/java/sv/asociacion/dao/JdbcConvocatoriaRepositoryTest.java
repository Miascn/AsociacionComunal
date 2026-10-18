package sv.asociacion.dao;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sv.asociacion.exception.ConexionBDException;
import sv.asociacion.exception.PersistenciaException;
import sv.asociacion.service.ConvocatoriaConcurrenteService;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;
import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JdbcConvocatoriaRepositoryTest {

    @Mock private DataSource dataSource;
    @Mock private Connection connection;
    @Mock private PreparedStatement statement;

    @Test
    @DisplayName("El SQL es idempotente y solo inscribe miembros activos")
    void sqlIdempotente() {
        assertTrue(JdbcConvocatoriaRepository.SQL_CONVOCAR.startsWith("INSERT IGNORE INTO asistencia"));
        assertTrue(JdbcConvocatoriaRepository.SQL_CONVOCAR.contains("m.estado = 'ACTIVO'"));
    }

    @Test
    @DisplayName("Un lote usa una conexion del DataSource, un batch de PreparedStatement y una transaccion")
    void loteEnUnaTransaccion() throws SQLException {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(JdbcConvocatoriaRepository.SQL_CONVOCAR)).thenReturn(statement);
        when(statement.executeBatch()).thenReturn(new int[]{1, 0, 1});

        int creados = new JdbcConvocatoriaRepository(dataSource).convocarLote(8, List.of(10, 20, 30));

        assertEquals(2, creados, "el miembro 20 ya estaba convocado: INSERT IGNORE informo 0 filas");
        verify(statement, times(3)).setInt(1, 8); // la reunion se asigna en cada fila del batch
        verify(statement).setInt(2, 10);
        verify(statement).setInt(2, 20);
        verify(statement).setInt(2, 30);
        verify(statement, times(3)).addBatch();
        verify(connection).setAutoCommit(false);
        verify(connection).commit();
        verify(connection).setAutoCommit(true);
        verify(connection).close();
    }

    @Test
    @DisplayName("Si el batch falla se deshace el lote, se restaura autoCommit y la conexion vuelve al pool")
    void falloDeshaceElLote() throws SQLException {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeBatch()).thenThrow(new SQLException("Lock wait timeout exceeded", "HY000", 1205));

        JdbcConvocatoriaRepository repo = new JdbcConvocatoriaRepository(dataSource);
        assertThrows(PersistenciaException.class, () -> repo.convocarLote(8, List.of(10)));

        verify(connection).rollback();
        verify(connection, never()).commit();
        verify(connection).setAutoCommit(true);
        verify(connection).close();
    }

    @Test
    @DisplayName("Sin conexion disponible el error es ConexionBDException")
    void sinConexion() throws SQLException {
        when(dataSource.getConnection()).thenThrow(new SQLException("Communications link failure", "08S01"));
        JdbcConvocatoriaRepository repo = new JdbcConvocatoriaRepository(dataSource);
        assertThrows(ConexionBDException.class, () -> repo.convocarLote(1, List.of(1)));
    }

    @Test
    @DisplayName("Lee los ids de miembros activos con executeQuery")
    void idsActivos() throws SQLException {
        Statement st = mock(Statement.class);
        ResultSet rs = mock(ResultSet.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(st);
        when(st.executeQuery(JdbcConvocatoriaRepository.SQL_ACTIVOS)).thenReturn(rs);
        when(rs.next()).thenReturn(true, true, false);
        when(rs.getInt(1)).thenReturn(4, 9);

        assertEquals(List.of(4, 9), new JdbcConvocatoriaRepository(dataSource).idsMiembrosActivos());
        verify(rs).close();
        verify(connection).close();
    }

    @Test
    @DisplayName("Integracion con el servicio concurrente: cada tarea obtiene y cierra su propia conexion")
    void cadaTareaUsaSuPropiaConexion() throws SQLException {
        Set<Connection> conexionesEntregadas = ConcurrentHashMap.newKeySet();
        when(dataSource.getConnection()).thenAnswer(inv -> {
            Connection c = mock(Connection.class);
            PreparedStatement ps = mock(PreparedStatement.class);
            when(c.prepareStatement(anyString())).thenReturn(ps);
            when(ps.executeBatch()).thenAnswer(b -> new int[]{1, 1, 1, 1, 1});
            conexionesEntregadas.add(c);
            return c;
        });
        var servicio = new ConvocatoriaConcurrenteService(
            new JdbcConvocatoriaRepository(dataSource), 4, 5, Duration.ofSeconds(10));

        var resultado = servicio.convocar(1, IntStream.rangeClosed(1, 40).boxed().toList());

        assertEquals(40, resultado.convocados());
        assertEquals(8, conexionesEntregadas.size(), "8 lotes: 8 prestamos distintos del DataSource");
        for (Connection c : conexionesEntregadas) {
            verify(c).commit();
            verify(c).close();
        }
    }
}
