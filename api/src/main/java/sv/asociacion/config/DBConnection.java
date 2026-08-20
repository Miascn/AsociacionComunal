package sv.asociacion.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;

public final class DBConnection {
    private static final Properties LOCAL_PROPERTIES = loadLocalProperties();
    private static final String DB_URL = buildUrl();
    private static final String DB_USER = requiredSetting("DB_USER", "db.user");
    private static final String DB_PASSWORD = requiredSetting("DB_PASSWORD", "db.password");
    private static DBConnection instance;

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException("No se encontró el controlador JDBC de MySQL.", exception);
        }
    }

    private DBConnection() {
    }

    public static synchronized DBConnection getInstance() {
        if (instance == null) {
            instance = new DBConnection();
        }
        return instance;
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
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
