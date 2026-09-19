package sv.asociacion.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.javalin.Javalin;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sv.asociacion.ApiServer;
import sv.asociacion.domain.dto.AportacionRequest;
import sv.asociacion.domain.dto.AportacionResponse;
import sv.asociacion.domain.dto.AsignacionCargoRequest;
import sv.asociacion.domain.dto.AsignacionCargoResponse;
import sv.asociacion.domain.dto.PeriodoRequest;
import sv.asociacion.domain.dto.PeriodoResponse;
import sv.asociacion.domain.dto.RevocarAsignacionRequest;
import sv.asociacion.domain.dto.VotacionRequest;
import sv.asociacion.domain.dto.VotacionResponse;
import sv.asociacion.domain.entity.Votacion;
import sv.asociacion.service.AportacionService;
import sv.asociacion.service.AsignacionCargoService;
import sv.asociacion.service.PeriodoService;
import sv.asociacion.service.VotacionService;

/**
 * Enlace de fechas JSON en rutas reales — SCRUM-331.
 *
 * <p>Cubre los cinco DTOs de entrada con tipos {@code java.time} y una ruta real por
 * cada uno. Antes del fix todas respondían 500 aunque la fecha fuese válida, porque el
 * mapeador por defecto de Jackson no trae soporte JSR-310.
 *
 * <p>El servidor se monta con {@link ApiServer#configurarJson} y
 * {@link ApiServer#configurarManejoErrores} —las mismas llamadas que hace producción, no
 * copias equivalentes—, de modo que lo que se prueba aquí es la configuración real. Si
 * esta clase registrase sus propios manejadores, mediría un servidor que no existe.
 */
class FechasJsonRoutesTest {

    private Javalin app;
    private HttpClient client;

    @BeforeEach
    void startServer() {
        VotacionController votaciones = new VotacionController(new StubVotacionService());
        AportacionController aportaciones = new AportacionController(new StubAportacionService());
        PeriodoController periodos = new PeriodoController(new StubPeriodoService());
        AsignacionCargoController asignaciones =
            new AsignacionCargoController(new StubAsignacionCargoService());

        app = Javalin.create(cfg -> {
            ApiServer.configurarJson(cfg);
            ApiServer.configurarManejoErrores(cfg);

            cfg.routes.before("/api/*", ctx -> ctx.attribute("role", "ADMIN"));

            cfg.routes.post("/api/votaciones", votaciones::create);
            cfg.routes.post("/api/aportaciones", aportaciones::create);
            cfg.routes.post("/api/periodos", periodos::create);
            cfg.routes.post("/api/asignaciones-cargo", asignaciones::create);
            cfg.routes.patch("/api/asignaciones-cargo/{id}/revocar", asignaciones::revocar);
        }).start("127.0.0.1", 0);

        client = HttpClient.newHttpClient();
    }

    @AfterEach
    void stopServer() {
        if (app != null) app.stop();
    }

    // ------------------------------------------------------------------
    // Fechas válidas: llegan al controlador y al servicio.
    // ------------------------------------------------------------------

    @Test
    void localDateTimeValidoAlcanzaElServicio() throws Exception {
        HttpResponse<String> res = send("POST", "/api/votaciones", votacion("2026-09-20T08:00:00"));
        assertEquals(201, res.statusCode(), "Una fecha-hora ISO válida debe procesarse: " + res.body());
    }

    @Test
    void localDateValidoAlcanzaElServicioEnLasCuatroRutas() throws Exception {
        assertEquals(201, send("POST", "/api/aportaciones", aportacion("2026-09-20")).statusCode(),
            "AportacionRequest.fechaPago");
        assertEquals(201, send("POST", "/api/periodos", periodo("2026-09-20")).statusCode(),
            "PeriodoRequest.fechaInicio");
        assertEquals(201, send("POST", "/api/asignaciones-cargo", asignacion("2026-09-20")).statusCode(),
            "AsignacionCargoRequest.fechaAsignacion");
        assertEquals(200, send("PATCH", "/api/asignaciones-cargo/1/revocar", revocacion("2026-09-20")).statusCode(),
            "RevocarAsignacionRequest.fechaFin");
    }

    @Test
    void laFechaValidaLlegaConSuValorAlServicio() throws Exception {
        // No basta con que no falle: el valor tiene que cruzar íntegro la frontera JSON.
        StubVotacionService servicio = new StubVotacionService();
        Javalin propio = Javalin.create(cfg -> {
            ApiServer.configurarJson(cfg);
            ApiServer.configurarManejoErrores(cfg);
            cfg.routes.before("/api/*", ctx -> ctx.attribute("role", "ADMIN"));
            cfg.routes.post("/api/votaciones", new VotacionController(servicio)::create);
        }).start("127.0.0.1", 0);

        try {
            client.send(HttpRequest
                    .newBuilder(URI.create("http://127.0.0.1:" + propio.port() + "/api/votaciones"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(votacion("2026-09-20T08:00:00")))
                    .build(),
                HttpResponse.BodyHandlers.ofString());

            assertEquals("2026-09-20T08:00", String.valueOf(servicio.ultimaFechaInicio),
                "La fecha debe llegar al servicio tal y como se envió.");
        } finally {
            propio.stop();
        }
    }

    // ------------------------------------------------------------------
    // Fechas inválidas: 400, nunca 500.
    // ------------------------------------------------------------------

    @Test
    void localDateTimeInvalidoProduceBadRequest() throws Exception {
        HttpResponse<String> res = send("POST", "/api/votaciones", votacion("no-es-fecha"));
        assertEquals(400, res.statusCode(), "Un valor no parseable es error del cliente.");
        assertTrue(res.body().contains("fechaInicio"),
            "El mensaje debe nombrar el campo culpable, pero fue: " + res.body());
    }

    @Test
    void localDateInvalidoProduceBadRequestEnLasCuatroRutas() throws Exception {
        assertEquals(400, send("POST", "/api/aportaciones", aportacion("20-09-2026")).statusCode(),
            "AportacionRequest.fechaPago");
        assertEquals(400, send("POST", "/api/periodos", periodo("20/09/2026")).statusCode(),
            "PeriodoRequest.fechaInicio");
        assertEquals(400, send("POST", "/api/asignaciones-cargo", asignacion("ayer")).statusCode(),
            "AsignacionCargoRequest.fechaAsignacion");
        assertEquals(400, send("PATCH", "/api/asignaciones-cargo/1/revocar", revocacion("2026-13-45")).statusCode(),
            "RevocarAsignacionRequest.fechaFin");
    }

    @Test
    void ningunaFechaInvalidaProduceErrorDeServidor() throws Exception {
        List<HttpResponse<String>> respuestas = List.of(
            send("POST", "/api/votaciones", votacion("no-es-fecha")),
            send("POST", "/api/aportaciones", aportacion("20-09-2026")),
            send("POST", "/api/periodos", periodo("20/09/2026")),
            send("POST", "/api/asignaciones-cargo", asignacion("ayer")),
            send("PATCH", "/api/asignaciones-cargo/1/revocar", revocacion("2026-13-45")));

        for (HttpResponse<String> res : respuestas) {
            assertTrue(res.statusCode() < 500,
                "El parsing de una fecha nunca debe dar error de servidor, pero dio "
                    + res.statusCode() + ": " + res.body());
        }
    }

    // ------------------------------------------------------------------
    // Cuerpos y utilidades
    // ------------------------------------------------------------------

    private static String votacion(String fechaInicio) {
        return "{\"titulo\":\"Consulta\",\"descripcion\":\"d\",\"idProyecto\":null,"
            + "\"fechaInicio\":\"" + fechaInicio + "\",\"fechaFin\":\"2026-09-25T17:00:00\","
            + "\"opcionesIniciales\":[\"A\",\"B\"]}";
    }

    private static String aportacion(String fechaPago) {
        return "{\"idMiembro\":1,\"idProyecto\":null,\"periodoMes\":\"2026-09\","
            + "\"monto\":25.00,\"fechaPago\":\"" + fechaPago + "\","
            + "\"metodoPago\":\"EFECTIVO\",\"referencia\":null}";
    }

    private static String periodo(String fechaInicio) {
        return "{\"nombre\":\"Periodo 2026\",\"fechaInicio\":\"" + fechaInicio + "\","
            + "\"fechaFin\":\"2027-09-20\",\"estado\":\"PLANIFICADO\"}";
    }

    private static String asignacion(String fechaAsignacion) {
        return "{\"idMiembro\":1,\"idCargo\":1,\"idPeriodo\":1,"
            + "\"fechaAsignacion\":\"" + fechaAsignacion + "\",\"fechaFin\":null}";
    }

    private static String revocacion(String fechaFin) {
        return "{\"fechaFin\":\"" + fechaFin + "\",\"motivo\":\"Renuncia\"}";
    }

    private HttpResponse<String> send(String method, String path, String body) throws Exception {
        return client.send(HttpRequest
                .newBuilder(URI.create("http://127.0.0.1:" + app.port() + path))
                .method(method, HttpRequest.BodyPublishers.ofString(body))
                .header("Content-Type", "application/json")
                .build(),
            HttpResponse.BodyHandlers.ofString());
    }

    // ------------------------------------------------------------------
    // Dobles. Esta clase verifica el enlace JSON de fechas, no reglas de negocio:
    // cada doble sobrescribe el único método que la ruta alcanza y devuelve una
    // respuesta fija, de modo que los DAO del constructor nunca se dereferencian.
    // ------------------------------------------------------------------

    private static final class StubVotacionService extends VotacionService {
        private Object ultimaFechaInicio;

        StubVotacionService() {
            super(null, null, null, null);
        }

        @Override
        public VotacionResponse create(VotacionRequest req) {
            ultimaFechaInicio = req.fechaInicio();
            return new VotacionResponse(1, req.titulo(), req.descripcion(), null, null,
                "", "", "BORRADOR", 0, 0, List.of());
        }

        @Override
        public List<VotacionResponse> findFiltered(Votacion.Estado e, Integer p, String b) {
            return List.of();
        }
    }

    private static final class StubAportacionService extends AportacionService {
        StubAportacionService() {
            super(null, null, null);
        }

        @Override
        public AportacionResponse create(AportacionRequest request) {
            return new AportacionResponse(1L, request.idMiembro(), "Miembro", "0000",
                null, null, request.periodoMes(), new BigDecimal("25.00"),
                String.valueOf(request.fechaPago()), "EFECTIVO", null, "REGISTRADA");
        }
    }

    private static final class StubPeriodoService extends PeriodoService {
        StubPeriodoService() {
            super(null);
        }

        @Override
        public PeriodoResponse create(PeriodoRequest request) {
            return new PeriodoResponse(1, request.nombre(),
                String.valueOf(request.fechaInicio()), String.valueOf(request.fechaFin()),
                "PLANIFICADO", false);
        }
    }

    private static final class StubAsignacionCargoService extends AsignacionCargoService {
        StubAsignacionCargoService() {
            super(null, null, null, null);
        }

        private static AsignacionCargoResponse muestra(String fechaAsignacion, String fechaFin) {
            return new AsignacionCargoResponse(1, 1, "Miembro", "0000", "0000-0000",
                1, "Presidente", 1, 1, "Periodo 2026", "ACTIVO",
                fechaAsignacion, fechaFin, null, "ACTIVO");
        }

        @Override
        public AsignacionCargoResponse create(AsignacionCargoRequest req) {
            return muestra(String.valueOf(req.fechaAsignacion()), String.valueOf(req.fechaFin()));
        }

        @Override
        public AsignacionCargoResponse revocar(Integer id, RevocarAsignacionRequest req) {
            return muestra("2026-09-20", req == null ? null : String.valueOf(req.fechaFin()));
        }
    }
}
