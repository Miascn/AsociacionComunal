package sv.asociacion;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import sv.asociacion.config.AppConfig;
import sv.asociacion.config.DBConnection;
import sv.asociacion.domain.dto.LoginRequest;
import sv.asociacion.domain.dto.LoginResponse;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class LoginIntegrationTest {
    private static final int TEST_PORT = 9080;
    private static final ObjectMapper JSON = new ObjectMapper();
    private static HttpClient httpClient;

    @BeforeAll
    static void setUp() throws Exception {
        System.setProperty("API_PORT", String.valueOf(TEST_PORT));
        System.setProperty("API_SHARED_SECRET", "abcdefghijklmnopqrstuvwxyz1234567890ab");
        System.setProperty("JWT_SECRET", "jwt-secret-for-testing-only-1234567890!!");

        AppConfig.load();
        assertTrue(dbAvailable(), "MySQL debe estar corriendo (docker-compose up -d)");

        httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

        new Thread(() -> ApiServer.main(new String[]{})).start();
        Thread.sleep(2000);
    }

    @AfterAll
    static void tearDown() {
        System.clearProperty("API_PORT");
        System.clearProperty("API_SHARED_SECRET");
        System.clearProperty("JWT_SECRET");
    }

    @Test
    void conexionBaseDeDatos() throws SQLException {
        try (Connection conn = DBConnection.getInstance().getConnection()) {
            assertTrue(conn.isValid(5), "La conexión a la base de datos debería ser válida");
        }
    }

    @Test
    void loginExitoso() throws Exception {
        String body = JSON.writeValueAsString(new LoginRequest("admin", "Admin2026!"));
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://127.0.0.1:" + TEST_PORT + "/api/auth/login"))
            .timeout(Duration.ofSeconds(10))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        LoginResponse login = JSON.readValue(response.body(), LoginResponse.class);
        assertNotNull(login.token());
        assertFalse(login.token().isBlank());
        assertEquals("ADMIN", login.role());
    }

    @Test
    void loginFallido() throws Exception {
        String body = JSON.writeValueAsString(new LoginRequest("admin", "wrong_password"));
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://127.0.0.1:" + TEST_PORT + "/api/auth/login"))
            .timeout(Duration.ofSeconds(10))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(401, response.statusCode());
    }

    private static boolean dbAvailable() {
        try (Connection conn = DBConnection.getInstance().getConnection()) {
            return conn.isValid(5);
        } catch (Exception e) {
            return false;
        }
    }
}