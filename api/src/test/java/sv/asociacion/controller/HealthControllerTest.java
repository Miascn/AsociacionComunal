package sv.asociacion.controller;

import io.javalin.Javalin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sv.asociacion.ApiServer;
import sv.asociacion.config.DBConnection;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;

class HealthControllerTest {

    private Javalin app;

    @AfterEach
    void detener() {
        if (app != null) app.stop();
    }

    @Test
    @DisplayName("Base disponible: 200 y estado del pool")
    void disponible() throws Exception {
        HttpResponse<String> res = get(new HealthController(() -> true, () -> new DBConnection.EstadoPool(true, 2, 3, 20)));
        assertEquals(200, res.statusCode());
        assertTrue(res.body().contains("\"database\":\"available\""), res.body());
        assertTrue(res.body().contains("\"activas\":2"), res.body());
        assertTrue(res.body().contains("\"maxActivas\":20"), res.body());
    }

    @Test
    @DisplayName("Base no disponible: 503")
    void noDisponible() throws Exception {
        HttpResponse<String> res = get(new HealthController(() -> false, () -> new DBConnection.EstadoPool(false, 0, 0, 20)));
        assertEquals(503, res.statusCode());
        assertTrue(res.body().contains("unavailable"));
    }

    private HttpResponse<String> get(HealthController controller) throws Exception {
        app = Javalin.create(cfg -> {
            ApiServer.configurarJson(cfg);
            cfg.routes.get("/health", controller::health);
        }).start("127.0.0.1", 0);
        HttpRequest req = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + app.port() + "/health")).GET().build();
        return HttpClient.newHttpClient().send(req, HttpResponse.BodyHandlers.ofString());
    }
}
