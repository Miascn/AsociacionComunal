package sv.asociacion.controller;

import io.javalin.Javalin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sv.asociacion.ApiServer;
import sv.asociacion.domain.dto.ConvocatoriaMasivaRequest;
import sv.asociacion.domain.dto.ResultadoConvocatoria;
import sv.asociacion.exception.ConexionBDException;
import sv.asociacion.exception.ConvocatoriaIncompletaException;
import sv.asociacion.service.AsistenciaService;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Ruta POST /api/reuniones/{id}/asistencias/convocatoria sobre Javalin real.
 * El servicio se reemplaza con un mock de Mockito: se prueba el controlador y el
 * mapeo central de errores, no la base de datos.
 */
class ConvocatoriaRoutesTest {

    private final AsistenciaService service = mock(AsistenciaService.class);
    private final HttpClient client = HttpClient.newHttpClient();
    private Javalin app;
    private volatile String rol = "ADMIN";

    @BeforeEach
    void iniciar() {
        AsistenciaController controller = new AsistenciaController(service);
        app = Javalin.create(cfg -> {
            ApiServer.configurarJson(cfg);
            ApiServer.configurarManejoErrores(cfg);
            cfg.routes.before("/api/*", ctx -> ctx.attribute("role", rol));
            cfg.routes.post("/api/reuniones/{idReunion}/asistencias/convocatoria", controller::convocarConResultado);
        }).start("127.0.0.1", 0);
    }

    @AfterEach
    void detener() {
        app.stop();
    }

    @Test
    @DisplayName("201 con el resultado agregado del proceso concurrente")
    void exito() throws Exception {
        when(service.convocarConResultado(eq(4), any()))
            .thenReturn(new ResultadoConvocatoria(4, 120, 118, 2, 5, 0, 4, 37, List.of()));

        HttpResponse<String> res = post("/api/reuniones/4/asistencias/convocatoria", "");

        assertEquals(201, res.statusCode(), res.body());
        assertTrue(res.body().contains("\"convocados\":118"), res.body());
        assertTrue(res.body().contains("\"hilos\":4"), res.body());
        assertTrue(res.body().contains("\"completa\":true"), res.body());
    }

    @Test
    @DisplayName("La lista explicita del cuerpo llega al servicio")
    void listaExplicita() throws Exception {
        when(service.convocarConResultado(eq(4), any()))
            .thenReturn(new ResultadoConvocatoria(4, 2, 2, 0, 1, 0, 1, 3, List.of()));

        post("/api/reuniones/4/asistencias/convocatoria", "{\"miembrosIds\":[10,20]}");

        verify(service).convocarConResultado(4, new ConvocatoriaMasivaRequest(List.of(10, 20)));
    }

    @Test
    @DisplayName("Un rol sin permiso recibe 403 y el servicio no se invoca")
    void sinPermiso() throws Exception {
        rol = "MIEMBRO";
        assertEquals(403, post("/api/reuniones/4/asistencias/convocatoria", "").statusCode());
        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("Reunion cancelada: la regla de negocio llega como 409")
    void reunionCancelada() throws Exception {
        when(service.convocarConResultado(eq(4), any()))
            .thenThrow(new IllegalStateException("No se pueden convocar miembros a una reunión en estado CANCELADA."));
        HttpResponse<String> res = post("/api/reuniones/4/asistencias/convocatoria", "");
        assertEquals(409, res.statusCode());
        assertTrue(res.body().contains("CANCELADA"));
    }

    @Test
    @DisplayName("Convocatoria incompleta por base caida: 503 con conteos y pendientes, sin SQL")
    void incompletaPorBaseCaida() throws Exception {
        ResultadoConvocatoria parcial = new ResultadoConvocatoria(4, 50, 25, 0, 2, 1, 2, 80, List.of(26, 27));
        when(service.convocarConResultado(eq(4), any())).thenThrow(new ConvocatoriaIncompletaException(parcial,
            new ConexionBDException("No fue posible convocar.", new java.sql.SQLException("Communications link failure", "08S01"))));

        HttpResponse<String> res = post("/api/reuniones/4/asistencias/convocatoria", "");

        assertEquals(503, res.statusCode());
        assertTrue(res.body().contains("\"miembrosPendientes\":[26,27]"), res.body());
        assertFalse(res.body().contains("Communications link failure"));
    }

    private HttpResponse<String> post(String ruta, String cuerpo) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + app.port() + ruta))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(cuerpo)).build();
        return client.send(req, HttpResponse.BodyHandlers.ofString());
    }
}
