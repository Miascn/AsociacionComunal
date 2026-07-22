package sv.asociacion.backend.config;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DBConnection {
    private static final String DB_URL;
    private static final String DB_USER;
    private static final String DB_PASSWORD;
    private static DBConnection instance;

    static {
        Properties props = new Properties();
        try (InputStream is = DBConnection.class.getResourceAsStream("/config.properties")) {
            if (is == null) {
                throw new RuntimeException("config.properties no encontrado en el classpath");
            }
            props.load(is);
            DB_URL = props.getProperty("db.url");
            DB_USER = props.getProperty("db.user");
            DB_PASSWORD = props.getProperty("db.password");
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (IOException e) {
            throw new RuntimeException("Error al cargar config.properties", e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("MySQL Driver not found", e);
        }
    }

    private DBConnection() {}

    public static synchronized DBConnection getInstance() {
        if (instance == null) {
            instance = new DBConnection();
        }
        return instance;
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }
}
