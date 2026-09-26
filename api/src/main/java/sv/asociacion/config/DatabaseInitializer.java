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
            );
            """;
        executeUpdate(createTables);

        String createConfig = """
            CREATE TABLE IF NOT EXISTS configuracion (
                clave VARCHAR(60) PRIMARY KEY,
                valor VARCHAR(255) NOT NULL,
                descripcion VARCHAR(255) NULL,
                actualizado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
            """;
        executeUpdate(createConfig);

        String insertDefaultConfig = """
            INSERT IGNORE INTO configuracion (clave, valor, descripcion)
            VALUES ('cuota_mantenimiento_mensual', '10.00', 'Monto de la cuota mensual de mantenimiento y vigilancia de la colonia');
            """;
        executeUpdate(insertDefaultConfig);

        try (Connection conn = dbConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            boolean hasCol = false;
            try (var rs = stmt.executeQuery("SHOW COLUMNS FROM usuario LIKE 'clave_temporal'")) {
                if (rs.next()) hasCol = true;
            }
            if (!hasCol) {
                stmt.executeUpdate("ALTER TABLE usuario ADD COLUMN clave_temporal VARCHAR(255) NULL AFTER requiere_cambio_clave");
            }
        } catch (SQLException e) {
            System.err.println("Notice on schema initialization (clave_temporal): " + e.getMessage());
        }
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
