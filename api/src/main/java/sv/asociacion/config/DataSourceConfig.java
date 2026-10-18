package sv.asociacion.config;

import org.apache.tomcat.jdbc.pool.DataSource;
import org.apache.tomcat.jdbc.pool.PoolProperties;

import java.util.Objects;

/**
 * Configuracion del pool de conexiones JDBC (Tomcat JDBC Pool).
 *
 * <p>Antes de esta clase, cada operacion abria una conexion nueva con
 * {@code DriverManager}: handshake TCP, autenticacion en MySQL y cierre fisico en
 * cada consulta. Con el pool, la aplicacion mantiene un conjunto acotado de
 * conexiones fisicas abiertas y las presta; {@code Connection.close()} ya no
 * cierra el socket, devuelve la conexion al pool.</p>
 *
 * <h2>Por que estos valores</h2>
 *
 * <p>El sistema lo usa la directiva de una sola asociacion comunal: unos pocos
 * administradores en el escritorio JavaFX y los residentes desde la app movil.
 * La carga normal son rafagas cortas de consultas simples (listados, registro de
 * pagos, votos). El unico proceso que pide varias conexiones a la vez es la
 * convocatoria concurrente a reuniones ({@code ConvocatoriaConcurrenteService}),
 * que usa como maximo {@link #HILOS_CONVOCATORIA} hilos. MySQL admite por defecto
 * 151 conexiones ({@code max_connections}); el pool se queda muy por debajo para
 * no competir con otras herramientas (respaldos, consola de administracion).</p>
 *
 * <p>Las credenciales nunca se escriben aqui: llegan desde {@link DBConnection},
 * que las lee de variables de entorno, propiedades del sistema o del archivo
 * local {@code database-local.properties} (no versionado).</p>
 */
public final class DataSourceConfig {

    /** Consulta minima y barata que obliga a MySQL a responder. */
    public static final String VALIDATION_QUERY = "SELECT 1";

    /** Segundos que puede tardar la consulta de validacion antes de dar la conexion por mala. */
    public static final int VALIDATION_QUERY_TIMEOUT_SEG = 3;

    /**
     * Conexiones que se abren al crear el pool. Cuatro cubren el uso tipico
     * (un par de administradores trabajando a la vez) sin esperar al primer handshake.
     */
    public static final int INITIAL_SIZE = 4;

    /** Minimo de conexiones inactivas que el evictor conserva; igual a INITIAL_SIZE para no oscilar. */
    public static final int MIN_IDLE = 4;

    /** Maximo de conexiones inactivas retenidas; el excedente tras una rafaga se cierra. */
    public static final int MAX_IDLE = 8;

    /**
     * Tope de conexiones prestadas a la vez. Veinte cubre los hilos de la
     * convocatoria concurrente mas las peticiones HTTP simultaneas esperables, y
     * deja a MySQL con margen de sobra frente a sus 151 conexiones por defecto.
     */
    public static final int MAX_ACTIVE = 20;

    /**
     * Milisegundos que una peticion espera una conexion libre cuando el pool esta
     * agotado. Diez segundos: suficiente para absorber una rafaga, corto para que
     * el cliente reciba un error (503) en lugar de quedar colgado.
     */
    public static final int MAX_WAIT_MS = 10_000;

    /** Validar al prestar: el cliente nunca recibe una conexion que MySQL ya cerro (wait_timeout). */
    public static final boolean TEST_ON_BORROW = true;

    /** No validar al devolver: duplicaria el costo sin beneficio, porque se valida al prestar. */
    public static final boolean TEST_ON_RETURN = false;

    /** Validar las inactivas en cada pasada del evictor para descartar las rotas antes de prestarlas. */
    public static final boolean TEST_WHILE_IDLE = true;

    /** Validar cada conexion fisica al crearla: un error de credenciales o de red se detecta de inmediato. */
    public static final boolean TEST_ON_CONNECT = true;

    /**
     * Intervalo minimo entre dos validaciones de una misma conexion. Con
     * testOnBorrow activo, sin este limite cada prestamo ejecutaria SELECT 1; con
     * 30 s, una conexion usada muchas veces seguidas se valida como mucho dos veces por minuto.
     */
    public static final long VALIDATION_INTERVAL_MS = 30_000;

    /** Cada 30 s corre el evictor: valida inactivas, cierra excedentes y detecta abandonadas. */
    public static final int TIME_BETWEEN_EVICTION_RUNS_MS = 30_000;

    /**
     * Una conexion inactiva por mas de 60 s es candidata a cerrarse (respetando
     * minIdle). Muy por debajo del wait_timeout de MySQL (8 h), asi el pool la
     * cierra antes de que lo haga el servidor.
     */
    public static final int MIN_EVICTABLE_IDLE_TIME_MS = 60_000;

    /** Recuperar conexiones que el codigo tomo y nunca cerro (fuga de recursos). */
    public static final boolean REMOVE_ABANDONED = true;

    /**
     * Segundos tras los cuales una conexion prestada se considera abandonada. Las
     * operaciones de este sistema duran milisegundos; 60 s solo los supera una fuga real.
     */
    public static final int REMOVE_ABANDONED_TIMEOUT_SEG = 60;

    /** Hilos de la convocatoria concurrente; debe quedar por debajo de MAX_ACTIVE. */
    public static final int HILOS_CONVOCATORIA = 4;

    /**
     * Interceptores: ConnectionState restablece autoCommit en cada prestamo (los
     * DAO transaccionales lo apagan) y StatementFinalizer cierra sentencias que
     * algun codigo olvido cerrar antes de devolver la conexion.
     */
    static final String JDBC_INTERCEPTORS =
        "org.apache.tomcat.jdbc.pool.interceptor.ConnectionState;"
            + "org.apache.tomcat.jdbc.pool.interceptor.StatementFinalizer";

    private DataSourceConfig() {
    }

    /**
     * Construye las propiedades del pool. No abre conexiones, por eso se puede
     * probar sin una base de datos.
     */
    public static PoolProperties construirPropiedades(String url, String username, String password) {
        Objects.requireNonNull(url, "La URL JDBC es obligatoria.");
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("El usuario de base de datos es obligatorio.");
        }

        PoolProperties p = new PoolProperties();
        p.setUrl(url);
        p.setDriverClassName("com.mysql.cj.jdbc.Driver");
        p.setUsername(username);
        p.setPassword(password == null ? "" : password);

        p.setValidationQuery(VALIDATION_QUERY);
        p.setValidationQueryTimeout(VALIDATION_QUERY_TIMEOUT_SEG);
        p.setInitialSize(INITIAL_SIZE);
        p.setMinIdle(MIN_IDLE);
        p.setMaxIdle(MAX_IDLE);
        p.setMaxActive(MAX_ACTIVE);
        p.setMaxWait(MAX_WAIT_MS);
        p.setTestOnBorrow(TEST_ON_BORROW);
        p.setTestOnReturn(TEST_ON_RETURN);
        p.setTestWhileIdle(TEST_WHILE_IDLE);
        p.setTestOnConnect(TEST_ON_CONNECT);
        p.setValidationInterval(VALIDATION_INTERVAL_MS);
        p.setTimeBetweenEvictionRunsMillis(TIME_BETWEEN_EVICTION_RUNS_MS);
        p.setMinEvictableIdleTimeMillis(MIN_EVICTABLE_IDLE_TIME_MS);
        p.setRemoveAbandoned(REMOVE_ABANDONED);
        p.setRemoveAbandonedTimeout(REMOVE_ABANDONED_TIMEOUT_SEG);
        p.setLogAbandoned(true);

        // autoCommit es el modo por defecto porque la mayoria de los DAO ejecutan una
        // sola sentencia. Los transaccionales lo apagan y, con el interceptor
        // ConnectionState, el siguiente prestamo vuelve a recibir autoCommit=true aunque
        // algun DAO olvide restaurarlo. (rollbackOnReturn no se usa: Tomcat solo lo
        // aplica cuando defaultAutoCommit es false.)
        p.setDefaultAutoCommit(true);
        p.setJdbcInterceptors(JDBC_INTERCEPTORS);
        p.setJmxEnabled(false);
        p.setName("asociacion-comunal-pool");
        return p;
    }

    /** Crea el DataSource con pool a partir de propiedades ya construidas. */
    public static DataSource crearDataSource(PoolProperties propiedades) {
        return new DataSource(Objects.requireNonNull(propiedades, "Las propiedades del pool son obligatorias."));
    }
}
