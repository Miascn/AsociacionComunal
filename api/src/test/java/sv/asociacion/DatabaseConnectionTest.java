package sv.asociacion;

import com.mysql.cj.jdbc.AbandonedConnectionCleanupThread;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import sv.asociacion.config.DBConnection;

public class DatabaseConnectionTest {
    public static void main(String[] args) {
        System.out.println("=== Test de Conexion a Base de Datos ===\n");

        try {
            System.out.println("1. Obteniendo conexion...");
            DBConnection db = DBConnection.getInstance();
            try (Connection conn = db.getConnection()) {
                System.out.println("   OK: Conexion establecida");

                System.out.println("\n2. Verificando que la base de datos existe...");
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT DATABASE()")) {
                    if (rs.next()) {
                        System.out.println("   Base de datos: " + rs.getString(1));
                    }
                }

                System.out.println("\n3. Listando tablas...");
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SHOW TABLES")) {
                    int count = 0;
                    while (rs.next()) {
                        System.out.println("   - " + rs.getString(1));
                        count++;
                    }
                    System.out.println("   Total: " + count + " tablas");
                }

                System.out.println("\n4. Verificando datos iniciales (roles)...");
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT * FROM rol")) {
                    while (rs.next()) {
                        System.out.println(
                            "   - ID: " + rs.getInt("id_rol")
                                + ", Nombre: " + rs.getString("nombre")
                        );
                    }
                }
            }

            System.out.println("\n=== TEST PASADO EXITOSAMENTE ===");
        } catch (Exception e) {
            System.err.println("\n=== TEST FALLIDO ===");
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        } finally {
            AbandonedConnectionCleanupThread.checkedShutdown();
        }
    }
}
