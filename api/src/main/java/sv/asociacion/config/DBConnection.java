package sv.asociacion.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;
import javax.sql.DataSource;

/**
 * Punto unico de acceso a conexiones de base de datos.
 *
 * <p>Desde el Avance 3 las conexiones salen de un pool ({@link DataSourceConfig}).
 * La firma de {@link #getConnection()} se conservo a proposito: los DAO siguen
 * escribiendo {@code try (Connection c = DBConnection.getInstance().getConnection())}
 * y el {@code close()} implicito del try-with-resources devuelve la conexion al
 * pool en lugar de cerrar el socket. Asi la migracion no obligo a reescribir
 * ningun DAO.</p>
 *
 * <p>El pool se crea de forma perezosa, en la primera solicitud: cargar la clase
 * no exige credenciales, lo que permite compilar y ejecutar las pruebas sin MySQL.</p>
 */
public final class DBConnection {
    private static final Properties LOCAL_PROPERTIES = loadLocalProperties();
    private static DBConnection instance;

    /** Pool compartido; volatile para publicarlo de forma segura entre hilos. */
    private volatile org.apache.tomcat.jdbc.pool.DataSource dataSource;

    private DBConnection() {
    }

    public static synchronized DBConnection getInstance() {
        if (instance == null) {
            instance = new DBConnection();
        }
        return instance;
    }

    /**
     * Presta una conexion del pool. Debe usarse siempre con try-with-resources
     * para que vuelva al pool aunque ocurra una excepcion.
     */
    public Connection getConnection() throws SQLException {
        return getDataSource().getConnection();
    }

    /**
     * DataSource con pool, para los componentes que reciben la fuente de
     * conexiones por constructor (por ejemplo, la convocatoria concurrente).
     */
    public DataSource getDataSource() throws SQLException {
        org.apache.tomcat.jdbc.pool.DataSource actual = dataSource;
        if (actual == null) {
            synchronized (this) {
                actual = dataSource;
                if (actual == null) {
                    actual = DataSourceConfig.crearDataSource(
                        DataSourceConfig.construirPropiedades(buildUrl(), requiredUser(), password())
                    );
                    dataSource = actual;
                }
            }
        }
        return actual;
    }

    /** Estado del pool para el endpoint de salud; no abre conexiones si el pool aun no existe. */
    public EstadoPool estadoPool() {
        org.apache.tomcat.jdbc.pool.DataSource actual = dataSource;
        if (actual == null) {
            return new EstadoPool(false, 0, 0, DataSourceConfig.MAX_ACTIVE);
        }
        return new EstadoPool(true, actual.getActive(), actual.getIdle(), actual.getMaxActive());
    }

    /** Comprueba que el pool puede entregar una conexion valida. */
    public boolean verificarDisponibilidad() {
        try (Connection connection = getConnection()) {
            return connection.isValid(DataSourceConfig.VALIDATION_QUERY_TIMEOUT_SEG);
        } catch (SQLException exception) {
            return false;
        }
    }

    /** Cierra el pool y todas sus conexiones fisicas (al apagar la API). */
    public synchronized void cerrar() {
        if (dataSource != null) {
            dataSource.close(true);
            dataSource = null;
        }
    }

    /** Fotografia del pool: si existe, conexiones prestadas, inactivas y el maximo configurado. */
    public record EstadoPool(boolean inicializado, int activas, int inactivas, int maxActivas) { }

    private static String requiredUser() throws SQLException {
        String user = setting("DB_USER", "db.user");
        if (user == null || user.isBlank()) {
            throw new SQLException(
                "Falta configurar DB_USER o la propiedad db.user en database-local.properties.", "08001"
            );
        }
        return user;
    }

    private static String password() {
        String password = setting("DB_PASSWORD", "db.password");
        return password == null ? "" : password;
    }

    private static String buildUrl() {
        String host = settingOrDefault("DB_HOST", "db.host", "localhost");
        String port = settingOrDefault("DB_PORT", "db.port", "3306");
        String database = settingOrDefault("DB_NAME", "db.name", "asociacion_comunal");
        String parameters = settingOrDefault(
            "DB_PARAMETERS",
            "db.parameters",
            "serverTimezone=America/El_Salvador&useUnicode=true&characterEncoding=UTF-8"
        );
        return "jdbc:mysql://" + host + ":" + port + "/" + database + "?" + parameters;
    }

    private static String requiredSetting(String environmentName, String propertyName) {
        String value = setting(environmentName, propertyName);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                "Falta configurar " + environmentName
                    + " o la propiedad " + propertyName
                    + " en database-local.properties."
            );
        }
        return value;
    }

    private static String settingOrDefault(
        String environmentName,
        String propertyName,
        String defaultValue
    ) {
        String value = setting(environmentName, propertyName);
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private static String setting(String environmentName, String propertyName) {
        String environmentValue = System.getenv(environmentName);
        if (environmentValue != null && !environmentValue.isBlank()) {
            return environmentValue.trim();
        }
        String systemValue = System.getProperty(environmentName);
        if (systemValue != null && !systemValue.isBlank()) {
            return systemValue.trim();
        }
        String propertyValue = LOCAL_PROPERTIES.getProperty(propertyName);
        return propertyValue == null ? null : propertyValue.trim();
    }

    private static Properties loadLocalProperties() {
        Properties properties = new Properties();
        Path configuredPath = configuredPropertiesPath();

        if (configuredPath == null) {
            return properties;
        }

        try (InputStream input = Files.newInputStream(configuredPath)) {
            properties.load(input);
            return properties;
        } catch (IOException exception) {
            throw new IllegalStateException(
                "No fue posible leer la configuración local: " + configuredPath.toAbsolutePath(),
                exception
            );
        }
    }

    private static Path configuredPropertiesPath() {
        String explicitPath = System.getenv("DB_CONFIG_FILE");
        if (explicitPath != null && !explicitPath.isBlank()) {
            Path path = Path.of(explicitPath.trim());
            if (!Files.isRegularFile(path)) {
                throw new IllegalStateException(
                    "El archivo indicado en DB_CONFIG_FILE no existe: " + path.toAbsolutePath()
                );
            }
            return path;
        }

        return List.of(
                Path.of("database-local.properties"),
                Path.of("..", "database-local.properties")
            )
            .stream()
            .filter(Files::isRegularFile)
            .findFirst()
            .orElse(null);
    }
}
