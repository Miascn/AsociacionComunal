package sv.asociacion.config;

import org.apache.tomcat.jdbc.pool.DataSource;
import org.apache.tomcat.jdbc.pool.PoolProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

class DataSourceConfigTest {

    private static final String URL = "jdbc:mysql://localhost:3306/asociacion_comunal";

    private DataSource pool;

    @AfterEach
    void cerrarPool() {
        if (pool != null) pool.close(true);
    }

    @Test
    @DisplayName("Configura los 15 parametros del pool exigidos por la guia con los valores documentados")
    void configuraTodosLosParametrosDelPool() {
        PoolProperties p = DataSourceConfig.construirPropiedades(URL, "app", "secreto");

        assertEquals("SELECT 1", p.getValidationQuery());
        assertEquals(4, p.getInitialSize());
        assertEquals(4, p.getMinIdle());
        assertEquals(8, p.getMaxIdle());
        assertEquals(20, p.getMaxActive());
        assertEquals(10_000, p.getMaxWait());
        assertTrue(p.isTestOnBorrow());
        assertFalse(p.isTestOnReturn());
        assertTrue(p.isTestWhileIdle());
        assertTrue(p.isTestOnConnect());
        assertEquals(30_000, p.getValidationInterval());
        assertEquals(30_000, p.getTimeBetweenEvictionRunsMillis());
        assertEquals(60_000, p.getMinEvictableIdleTimeMillis());
        assertTrue(p.isRemoveAbandoned());
        assertEquals(60, p.getRemoveAbandonedTimeout());
    }

    @Test
    @DisplayName("Los valores son coherentes entre si: minIdle <= maxIdle <= maxActive y los hilos caben en el pool")
    void valoresCoherentes() {
        assertTrue(DataSourceConfig.INITIAL_SIZE <= DataSourceConfig.MAX_ACTIVE);
        assertTrue(DataSourceConfig.MIN_IDLE <= DataSourceConfig.MAX_IDLE);
        assertTrue(DataSourceConfig.MAX_IDLE <= DataSourceConfig.MAX_ACTIVE);
        assertTrue(DataSourceConfig.HILOS_CONVOCATORIA < DataSourceConfig.MAX_ACTIVE,
            "la convocatoria concurrente no debe poder agotar el pool por si sola");
        assertTrue(DataSourceConfig.MIN_EVICTABLE_IDLE_TIME_MS > DataSourceConfig.TIME_BETWEEN_EVICTION_RUNS_MS);
    }

    @Test
    @DisplayName("Las credenciales llegan por parametro: sin usuario no se construye el pool")
    void exigeUsuario() {
        assertThrows(IllegalArgumentException.class, () -> DataSourceConfig.construirPropiedades(URL, " ", "x"));
        assertThrows(NullPointerException.class, () -> DataSourceConfig.construirPropiedades(null, "app", "x"));
        assertEquals("", DataSourceConfig.construirPropiedades(URL, "app", null).getPassword());
    }

    @Test
    @DisplayName("close() devuelve la conexion al pool: 10 prestamos reutilizan las mismas conexiones fisicas")
    void closeDevuelveLaConexionAlPool() throws SQLException {
        FakeJdbc fake = new FakeJdbc();
        pool = poolSobre(fake, DataSourceConfig.construirPropiedades(URL, "app", "x"));

        for (int i = 0; i < 10; i++) {
            try (Connection c = pool.getConnection()) {
                assertFalse(c.isClosed());
                assertEquals(1, pool.getActive());
            }
            assertEquals(0, pool.getActive(), "tras close() la conexion ya no esta prestada");
        }

        assertEquals(DataSourceConfig.INITIAL_SIZE, fake.fisicasCreadas.get(),
            "solo se abrieron las conexiones iniciales; los 10 prestamos las reutilizaron");
        assertEquals(0, fake.fisicasCerradas.get(), "ninguna conexion fisica se cerro al llamar close()");
    }

    @Test
    @DisplayName("maxActive limita las conexiones simultaneas y maxWait corta la espera con SQLException")
    void respetaMaxActiveYMaxWait() throws SQLException {
        FakeJdbc fake = new FakeJdbc();
        PoolProperties p = DataSourceConfig.construirPropiedades(URL, "app", "x");
        p.setInitialSize(1);
        p.setMinIdle(1);
        p.setMaxIdle(2);
        p.setMaxActive(2);
        p.setMaxWait(200);
        pool = poolSobre(fake, p);

        try (Connection a = pool.getConnection(); Connection b = pool.getConnection()) {
            assertNotNull(a);
            assertNotNull(b);
            long inicio = System.nanoTime();
            assertThrows(SQLException.class, pool::getConnection, "el pool agotado no presta una tercera conexion");
            long esperaMs = (System.nanoTime() - inicio) / 1_000_000;
            assertTrue(esperaMs >= 150, "espero aproximadamente maxWait antes de fallar: " + esperaMs + " ms");
        }
        assertEquals(2, fake.fisicasCreadas.get());
    }

    @Test
    @DisplayName("Un DAO que apaga autoCommit no contamina al siguiente prestamo: ConnectionState lo restablece")
    void restableceAutoCommitEnElSiguientePrestamo() throws SQLException {
        FakeJdbc fake = new FakeJdbc();
        PoolProperties p = DataSourceConfig.construirPropiedades(URL, "app", "x");
        p.setInitialSize(1);
        p.setMinIdle(1);
        p.setMaxIdle(1);
        pool = poolSobre(fake, p);

        try (Connection c = pool.getConnection()) {
            c.setAutoCommit(false);
            c.rollback();
        }

        try (Connection c = pool.getConnection()) {
            assertTrue(c.getAutoCommit(), "el siguiente usuario de la misma conexion fisica recibe autoCommit=true");
        }
        assertEquals(1, fake.fisicasCreadas.get(), "fue la misma conexion fisica, reutilizada");
    }

    private static DataSource poolSobre(FakeJdbc fake, PoolProperties p) {
        p.setDataSource(fake.dataSource());
        return DataSourceConfig.crearDataSource(p);
    }
}
