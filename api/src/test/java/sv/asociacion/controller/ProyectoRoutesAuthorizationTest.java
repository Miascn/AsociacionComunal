package sv.asociacion.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.javalin.Javalin;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sv.asociacion.domain.dto.ProyectoRequest;
import sv.asociacion.domain.dto.ProyectoResponse;
import sv.asociacion.domain.entity.Proyecto;
import sv.asociacion.service.ProyectoService;

/**
 * Autorización de las rutas de proyectos — criterio "permisos" de SCRUM-253 y SCRUM-258.
 *
 * <p>Sigue el patrón de {@code AuthRoutesTest}: levanta un Javalin real en un puerto
 * efímero y emite peticiones HTTP verdaderas contra los manejadores reales de
 * {@link ProyectoController}.
 *
 * <p>El rol se inyecta mediante un {@code before} que copia una cabecera de prueba al
 * atributo {@code role}, que es exactamente lo que hace {@code JwtAuthMiddleware:41} en
 * producción. Se prueba así la regla de autorización —qué roles pueden gestionar
 * proyectos— sin emitir JWT, cuya generación y validación ya tienen cobertura propia en
 * {@code AuthServiceTest} y {@code AuthRoutesTest}.
 */
class ProyectoRoutesAuthorizationTest {

    /** Roles que {@code ProyectoController.canManage} acepta. */
    private static final List<String> ROLES_CON_GESTION =
        List.of("ADMIN", "ADMINISTRADOR", "PRESIDENTE", "TESORERO");

    /**
     * Roles autenticados que no deben poder gestionar proyectos.
     *
     * <p>{@code DIRECTIVO} figura aquí porque la autorización actual no lo reconoce pese a
     * estar sembrado en el catálogo. Es el comportamiento vigente y esta prueba lo fija;
     * la decisión de diseño sobre si debería cambiar está registrada en SCRUM-179 y queda
     * fuera de SCRUM-253.
     */
    private static final List<String> ROLES_SIN_GESTION =
        List.of("MIEMBRO", "DIRECTIVO", "SECRETARIO", "SINDICO");

    private Javalin app;
    private HttpClient client;
    private StubProyectoService service;

    @BeforeEach
    void startServer() {
        service = new StubProyectoService();
        ProyectoController controller = new ProyectoController(service);

        app = Javalin.create(config -> {
            config.routes.before("/api/*", context -> {
                String role = context.header("X-Test-Role");
                if (role != null && !role.isBlank()) {
                    context.attribute("role", role);
                }
            });
            config.routes.get("/api/proyectos", controller::getAll);
            config.routes.get("/api/proyectos/{id}", controller::getById);
            config.routes.post("/api/proyectos", controller::create);
            config.routes.put("/api/proyectos/{id}", controller::update);
            config.routes.patch("/api/proyectos/{id}/estado", controller::changeState);
            config.routes.delete("/api/proyectos/{id}", controller::delete);
        }).start("127.0.0.1", 0);

        client = HttpClient.newHttpClient();
    }

    @AfterEach
    void stopServer() {
        if (app != null) app.stop();
    }

    @Test
    void rolesDeGestionPuedenEscribir() throws Exception {
        for (String role : ROLES_CON_GESTION) {
            assertEquals(201, send("POST", "/api/proyectos", role, cuerpoProyecto()).statusCode(),
                "POST debería permitirse al rol " + role);
            assertEquals(200, send("PUT", "/api/proyectos/1", role, cuerpoProyecto()).statusCode(),
                "PUT debería permitirse al rol " + role);
            assertEquals(200, send("PATCH", "/api/proyectos/1/estado", role, "{\"nuevoEstado\":\"PROPUESTO\"}").statusCode(),
                "PATCH de estado debería permitirse al rol " + role);
            assertEquals(200, send("DELETE", "/api/proyectos/1", role, null).statusCode(),
                "DELETE debería permitirse al rol " + role);
        }
    }

    @Test
    void rolesSinGestionRecibenForbidden() throws Exception {
        for (String role : ROLES_SIN_GESTION) {
            assertEquals(403, send("POST", "/api/proyectos", role, cuerpoProyecto()).statusCode(),
                "POST debería rechazarse al rol " + role);
            assertEquals(403, send("PUT", "/api/proyectos/1", role, cuerpoProyecto()).statusCode(),
                "PUT debería rechazarse al rol " + role);
            assertEquals(403, send("PATCH", "/api/proyectos/1/estado", role, "{\"nuevoEstado\":\"PROPUESTO\"}").statusCode(),
                "PATCH de estado debería rechazarse al rol " + role);
            assertEquals(403, send("DELETE", "/api/proyectos/1", role, null).statusCode(),
                "DELETE debería rechazarse al rol " + role);
        }
    }

    @Test
    void peticionSinRolRecibeForbidden() throws Exception {
        assertEquals(403, send("POST", "/api/proyectos", null, cuerpoProyecto()).statusCode());
        assertEquals(403, send("PUT", "/api/proyectos/1", null, cuerpoProyecto()).statusCode());
        assertEquals(403, send("PATCH", "/api/proyectos/1/estado", null, "{\"nuevoEstado\":\"PROPUESTO\"}").statusCode());
        assertEquals(403, send("DELETE", "/api/proyectos/1", null, null).statusCode());
    }

    @Test
    void escrituraDenegadaNoInvocaElServicio() throws Exception {
        send("POST", "/api/proyectos", "MIEMBRO", cuerpoProyecto());
        send("PUT", "/api/proyectos/1", "MIEMBRO", cuerpoProyecto());
        send("PATCH", "/api/proyectos/1/estado", "MIEMBRO", "{\"nuevoEstado\":\"PROPUESTO\"}");
        send("DELETE", "/api/proyectos/1", "MIEMBRO", null);

        assertTrue(service.invocaciones.isEmpty(),
            "La guarda debe cortar antes de la lógica de negocio, pero se invocó: " + service.invocaciones);
    }

    @Test
    void escrituraPermitidaSiInvocaElServicio() throws Exception {
        send("DELETE", "/api/proyectos/7", "PRESIDENTE", null);
        assertEquals(List.of("delete"), service.invocaciones);
    }

    @Test
    void elNombreDelRolSeNormaliza() throws Exception {
        // canManage recorta y pasa a mayúsculas: la autorización no debe depender
        // de cómo venga escrito el rol en el token.
        assertEquals(201, send("POST", "/api/proyectos", "  admin  ", cuerpoProyecto()).statusCode());
        assertEquals(201, send("POST", "/api/proyectos", "Presidente", cuerpoProyecto()).statusCode());
    }

    @Test
    void lecturaDisponibleParaRolSinGestion() throws Exception {
        // getAll y getById no exigen canManage: cualquier usuario autenticado consulta
        // el listado y el detalle. Se fija el comportamiento vigente.
        assertNotEquals(403, send("GET", "/api/proyectos", "MIEMBRO", null).statusCode());
        assertNotEquals(403, send("GET", "/api/proyectos/1", "MIEMBRO", null).statusCode());
    }

    private static String cuerpoProyecto() {
        return "{\"nombre\":\"Proyecto de prueba\",\"descripcion\":\"Autorización\","
            + "\"presupuesto\":100.00,\"creadoPor\":1,\"estado\":\"BORRADOR\"}";
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
     * Doble de prueba: esta clase verifica autorización de rutas, no reglas de negocio.
     * Todos los métodos que el controlador invoca están sobrescritos, de modo que los DAO
     * del constructor nunca se dereferencian. Registra cada invocación para poder afirmar
     * que una petición denegada no llega a la lógica de negocio.
     */
    private static final class StubProyectoService extends ProyectoService {
        private final List<String> invocaciones = new ArrayList<>();

        StubProyectoService() {
            super(null, null, null, null);
        }

        private static ProyectoResponse muestra() {
            return new ProyectoResponse(
                1, 1, "admin", "Proyecto de prueba", "Autorización",
                new BigDecimal("100.00"), "2026-09-15", "BORRADOR"
            );
        }

        @Override
        public List<ProyectoResponse> findFiltered(Proyecto.Estado estado, Integer creadoPor, String busqueda) {
            invocaciones.add("findFiltered");
            return List.of(muestra());
        }

        @Override
        public ProyectoResponse findById(Integer id) {
            invocaciones.add("findById");
            return muestra();
        }

        @Override
        public ProyectoResponse create(ProyectoRequest request) {
            invocaciones.add("create");
            return muestra();
        }

        @Override
        public ProyectoResponse update(Integer id, ProyectoRequest request) {
            invocaciones.add("update");
            return muestra();
        }

        @Override
        public ProyectoResponse cambiarEstado(Integer id, Proyecto.Estado nuevoEstado) {
            invocaciones.add("cambiarEstado");
            return muestra();
        }

        @Override
        public boolean delete(Integer id) {
            invocaciones.add("delete");
            return true;
        }
    }
}
