package sv.asociacion.backend.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class DBConnection {
    private static final String DB_URL = buildUrl();
    private static final String DB_USER = requiredEnvironmentVariable("DB_USER");
    private static final String DB_PASSWORD = requiredEnvironmentVariable("DB_PASSWORD");
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
        String host = environmentVariableOrDefault("DB_HOST", "localhost");
        String port = environmentVariableOrDefault("DB_PORT", "3306");
        String database = environmentVariableOrDefault("DB_NAME", "asociacion_comunal");
        return "jdbc:mysql://" + host + ":" + port + "/" + database;
    }

    private static String requiredEnvironmentVariable(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Falta configurar la variable de entorno " + name + ".");
        }
        return value;
    }

    private static String environmentVariableOrDefault(String name, String defaultValue) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
