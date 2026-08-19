package sv.asociacion.config;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseInitializer {
    private final DBConnection dbConnection;

    public DatabaseInitializer() {
        this.dbConnection = DBConnection.getInstance();
    }

    public void initializeSchema() {
        String createTables = """
            CREATE TABLE IF NOT EXISTS rol (
                id_rol INT AUTO_INCREMENT PRIMARY KEY,
                nombre VARCHAR(40) NOT NULL UNIQUE,
                descripcion VARCHAR(200)
            )
            """;
        executeUpdate(createTables);
    }

    public boolean testConnection() {
        try (Connection conn = dbConnection.getConnection()) {
            return conn.isValid(5);
        } catch (SQLException e) {
            System.err.println("Connection failed: " + e.getMessage());
            return false;
        }
    }

    private void executeUpdate(String sql) {
        try (Connection conn = dbConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            System.err.println("SQL Error: " + e.getMessage());
        }
    }
}
