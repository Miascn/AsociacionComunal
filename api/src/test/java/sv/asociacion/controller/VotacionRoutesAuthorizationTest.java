package sv.asociacion.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.javalin.Javalin;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sv.asociacion.ApiServer;
import sv.asociacion.domain.dto.VotacionRequest;
import sv.asociacion.domain.dto.VotacionResponse;
import sv.asociacion.domain.entity.Votacion;
import sv.asociacion.service.VotacionService;

/**
 * Autorización de las rutas de votaciones — criterio "permisos" de SCRUM-260.
 *
 * <p>Sigue el patrón de {@code ProyectoRoutesAuthorizationTest}: Javalin real en puerto
 * efímero, peticiones HTTP verdaderas contra los manejadores reales de
 * {@link VotacionController}, y el rol inyectado por un {@code before} que replica lo que
 * {@code JwtAuthMiddleware:41} hace en producción.
 *
 * <p>El servidor se monta con {@link ApiServer#configurarJson} y
 * {@link ApiServer#configurarManejoErrores}, la misma configuración que usa producción,
 * de modo que las escrituras con cuerpo se afirman con su código exacto. Hasta SCRUM-331
 * se afirmaban como "no 403", porque el mapeador por defecto no sabía leer
 * {@code LocalDateTime} y devolvían 500; ese defecto ya está corregido.
 */
class VotacionRoutesAuthorizationTest {

    /**
     * Roles que {@code VotacionController.canManage} acepta.
     *
     * <p>Es una lista más corta que la de proyectos: {@code TESORERO} gestiona proyectos
     * pero no votaciones. Se fija el comportamiento vigente.
     */
    private static final List<String> ROLES_CON_GESTION =
        List.of("ADMIN", "ADMINISTRADOR", "PRESIDENTE");

    /**
     * Roles autenticados que no deben poder gestionar votaciones.
     *
     * <p>{@code DIRECTIVO} figura aquí porque la autorización no lo reconoce pese a estar
     * sembrado en el catálogo; la decisión de diseño está registrada en SCRUM-179 y queda
     * fuera de SCRUM-260.
     */
    private static final List<String> ROLES_SIN_GESTION =
        List.of("MIEMBRO", "DIRECTIVO", "SECRETARIO", "SINDICO", "TESORERO");

    private Javalin app;
    private HttpClient client;
    private StubVotacionService service;

    @BeforeEach
    void startServer() {
        service = new StubVotacionService();
        VotacionController controller = new VotacionController(service);

        app = Javalin.create(config -> {
            ApiServer.configurarJson(config);
            ApiServer.configurarManejoErrores(config);
            config.routes.before("/api/*", context -> {
                String role = context.header("X-Test-Role");
                if (role != null && !role.isBlank()) {
                    context.attribute("role", role);
                }
            });
            config.routes.get("/api/votaciones", controller::getAll);
            config.routes.get("/api/votaciones/{id}", controller::getById);
            config.routes.post("/api/votaciones", controller::create);
            config.routes.put("/api/votaciones/{id}", controller::update);
            config.routes.patch("/api/votaciones/{id}/abrir", controller::abrir);
            config.routes.patch("/api/votaciones/{id}/cerrar", controller::cerrar);
            config.routes.patch("/api/votaciones/{id}/cancelar", controller::cancelar);
            config.routes.delete("/api/votaciones/{id}", controller::delete);
        }).start("127.0.0.1", 0);

        client = HttpClient.newHttpClient();
    }

    @AfterEach
    void stopServer() {
        if (app != null) app.stop();
    }

    @Test
    void rolesDeGestionControlanElCicloDeVida() throws Exception {
        for (String role : ROLES_CON_GESTION) {
            assertEquals(200, send("PATCH", "/api/votaciones/1/abrir", role, null).statusCode(),
                "Abrir debería permitirse al rol " + role);
            assertEquals(200, send("PATCH", "/api/votaciones/1/cerrar", role, null).statusCode(),
                "Cerrar debería permitirse al rol " + role);
            assertEquals(200, send("PATCH", "/api/votaciones/1/cancelar", role, null).statusCode(),
                "Cancelar debería permitirse al rol " + role);
            assertEquals(200, send("DELETE", "/api/votaciones/1", role, null).statusCode(),
                "Eliminar debería permitirse al rol " + role);

            assertEquals(201, send("POST", "/api/votaciones", role, cuerpoVotacion()).statusCode(),
                "Crear debería permitirse al rol " + role);
            assertEquals(200, send("PUT", "/api/votaciones/1", role, cuerpoVotacion()).statusCode(),
                "Editar debería permitirse al rol " + role);
        }
    }

    @Test
    void rolesSinGestionRecibenForbidden() throws Exception {
        for (String role : ROLES_SIN_GESTION) {
            assertEquals(403, send("POST", "/api/votaciones", role, cuerpoVotacion()).statusCode(),
                "Crear debería rechazarse al rol " + role);
            assertEquals(403, send("PUT", "/api/votaciones/1", role, cuerpoVotacion()).statusCode(),
                "Editar debería rechazarse al rol " + role);
            assertEquals(403, send("PATCH", "/api/votaciones/1/abrir", role, null).statusCode(),
                "Abrir debería rechazarse al rol " + role);
            assertEquals(403, send("PATCH", "/api/votaciones/1/cerrar", role, null).statusCode(),
                "Cerrar debería rechazarse al rol " + role);
            assertEquals(403, send("PATCH", "/api/votaciones/1/cancelar", role, null).statusCode(),
                "Cancelar debería rechazarse al rol " + role);
            assertEquals(403, send("DELETE", "/api/votaciones/1", role, null).statusCode(),
                "Eliminar debería rechazarse al rol " + role);
        }
    }

    @Test
    void peticionSinRolRecibeForbidden() throws Exception {
        assertEquals(403, send("POST", "/api/votaciones", null, cuerpoVotacion()).statusCode());
        assertEquals(403, send("PUT", "/api/votaciones/1", null, cuerpoVotacion()).statusCode());
        assertEquals(403, send("PATCH", "/api/votaciones/1/abrir", null, null).statusCode());
        assertEquals(403, send("PATCH", "/api/votaciones/1/cerrar", null, null).statusCode());
        assertEquals(403, send("PATCH", "/api/votaciones/1/cancelar", null, null).statusCode());
        assertEquals(403, send("DELETE", "/api/votaciones/1", null, null).statusCode());
    }

    @Test
    void laDenegacionCortaAntesDeLaLogicaDeNegocio() throws Exception {
        send("POST", "/api/votaciones", "MIEMBRO", cuerpoVotacion());
        send("PUT", "/api/votaciones/1", "MIEMBRO", cuerpoVotacion());
        send("PATCH", "/api/votaciones/1/abrir", "MIEMBRO", null);
        send("PATCH", "/api/votaciones/1/cerrar", "MIEMBRO", null);
        send("PATCH", "/api/votaciones/1/cancelar", "MIEMBRO", null);
        send("DELETE", "/api/votaciones/1", "MIEMBRO", null);

        assertTrue(service.invocaciones.isEmpty(),
            "La guarda debe cortar antes del servicio, pero se invocó: " + service.invocaciones);
    }

    @Test
    void laOperacionPermitidaSiAlcanzaElServicio() throws Exception {
        send("PATCH", "/api/votaciones/7/cerrar", "PRESIDENTE", null);
        assertEquals(List.of("cerrar"), service.invocaciones);
    }

    @Test
    void elNombreDelRolSeNormaliza() throws Exception {
        // canManage recorta y pasa a mayúsculas: la autorización no depende de cómo
        // venga escrito el rol en el token.
        assertEquals(200, send("PATCH", "/api/votaciones/1/abrir", "  admin  ", null).statusCode());
        assertEquals(200, send("PATCH", "/api/votaciones/1/abrir", "Presidente", null).statusCode());
    }

    @Test
    void laConsultaDeResultadosEstaDisponibleParaRolSinGestion() throws Exception {
        // getAll y getById no exigen canManage: cualquier miembro autenticado consulta
        // los resultados publicados. Se fija el comportamiento vigente.
        assertNotEquals(403, send("GET", "/api/votaciones", "MIEMBRO", null).statusCode());
        assertNotEquals(403, send("GET", "/api/votaciones/1", "MIEMBRO", null).statusCode());
    }

    private static String cuerpoVotacion() {
        return "{\"titulo\":\"Consulta de autorización\",\"descripcion\":\"Permisos\","
            + "\"idProyecto\":null,\"fechaInicio\":\"2026-09-20T08:00:00\","
            + "\"fechaFin\":\"2026-09-25T17:00:00\",\"opcionesIniciales\":[\"A\",\"B\"]}";
    }

    private HttpResponse<String> send(String method, String path, String role, String body) throws Exception {
        HttpRequest.BodyPublisher publisher = body == null
            ? HttpRequest.BodyPublishers.noBody()
            : HttpRequest.BodyPublishers.ofString(body);

        HttpRequest.Builder request = HttpRequest
            .newBuilder(URI.create("http://127.0.0.1:" + app.port() + path))
            .method(method, publisher)
            .header("Content-Type", "application/json");

        if (role != null) {
            request.header("X-Test-Role", role);
        }
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    /**
     * Doble de prueba: esta clase verifica autorización, no reglas de negocio. Todos los
     * métodos que el controlador invoca están sobrescritos, de modo que los DAO del
     * constructor nunca se dereferencian. Registra cada invocación para poder afirmar que
     * una petición denegada no llega a la lógica de negocio.
     */
    private static final class StubVotacionService extends VotacionService {
        private final List<String> invocaciones = new ArrayList<>();

        StubVotacionService() {
            super(null, null, null, null);
        }

        private static VotacionResponse muestra() {
            return new VotacionResponse(
                1, "Consulta de autorización", "Permisos", null, null,
                "2026-09-20T08:00", "2026-09-25T17:00", "BORRADOR", 2, 0,
                List.of(new VotacionResponse.OpcionDetalle(1, "A", (short) 1, 0, 0.0))
            );
        }

        @Override
        public List<VotacionResponse> findFiltered(Votacion.Estado estado, Integer idProyecto, String busqueda) {
            invocaciones.add("findFiltered");
            return List.of(muestra());
        }

        @Override
        public VotacionResponse findById(Integer id) {
            invocaciones.add("findById");
            return muestra();
        }

        @Override
        public VotacionResponse create(VotacionRequest req) {
            invocaciones.add("create");
            return muestra();
        }

        @Override
        public VotacionResponse update(Integer id, VotacionRequest req) {
            invocaciones.add("update");
            return muestra();
        }

        @Override
        public VotacionResponse abrir(Integer id) {
            invocaciones.add("abrir");
            return muestra();
        }

        @Override
        public VotacionResponse cerrar(Integer id) {
            invocaciones.add("cerrar");
            return muestra();
        }

        @Override
        public VotacionResponse cancelar(Integer id) {
            invocaciones.add("cancelar");
            return muestra();
        }

        @Override
        public boolean delete(Integer id) {
            invocaciones.add("delete");
            return true;
        }
    }
}
