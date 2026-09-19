package sv.asociacion.controller;

import io.javalin.Javalin;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sv.asociacion.dao.AsistenciaDAO;
import sv.asociacion.dao.ReunionDAO;
import sv.asociacion.domain.entity.Asistencia;
import sv.asociacion.domain.entity.Reunion;
import sv.asociacion.service.ReunionService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReunionControllerTest {
    private Javalin app;
    private HttpClient client;
    private MemoryReunionDAO reunionDAO;
    private MemoryAsistenciaDAO asistenciaDAO;

    @BeforeEach
    void setUp() {
        reunionDAO = new MemoryReunionDAO();
        asistenciaDAO = new MemoryAsistenciaDAO();
        ReunionService service = new ReunionService(reunionDAO, asistenciaDAO);
        ReunionController controller = new ReunionController(service);

        app = Javalin.create(config -> {
            config.routes.before(ctx -> {
                String role = ctx.header("X-Role");
                if (role != null) {
                    ctx.attribute("role", role);
                }
            });
            config.routes.get("/api/reuniones/{id}", controller::getById);
            config.routes.post("/api/reuniones", controller::create);
            config.routes.put("/api/reuniones/{id}", controller::update);
            config.routes.patch("/api/reuniones/{id}/realizada", controller::marcarRealizada);
            config.routes.patch("/api/reuniones/{id}/cancelar", controller::cancelar);
            config.routes.delete("/api/reuniones/{id}", controller::delete);
        }).start("127.0.0.1", 0);

        client = HttpClient.newHttpClient();
    }

    @AfterEach
    void tearDown() {
        if (app != null) {
            app.stop();
        }
    }

    private String url(String path) {
        return "http://127.0.0.1:" + app.port() + path;
    }

    @Test
    void getByIdReturns404WhenNotFound() throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(url("/api/reuniones/999"))).GET().build();
        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(404, res.statusCode());
        assertTrue(res.body().contains("no encontrada"));
    }

    @Test
    void createRechazaRolNoAutorizadoCon403() throws Exception {
        String json = """
            {"titulo":"Asamblea","fechaHora":"2026-10-15 14:00:00","lugar":"Cancha","tipo":"ORDINARIA"}
            """;
        HttpRequest req = HttpRequest.newBuilder(URI.create(url("/api/reuniones")))
            .header("Content-Type", "application/json")
            .header("X-Role", "MIEMBRO")
            .POST(HttpRequest.BodyPublishers.ofString(json))
            .build();
        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(403, res.statusCode());
    }

    @Test
    void createPermiteRolesAutorizadosCon201() throws Exception {
        String json = """
            {"titulo":"Asamblea","fechaHora":"2026-10-15 14:00:00","lugar":"Cancha","tipo":"ORDINARIA"}
            """;
        for (String role : List.of("ADMIN", "ADMINISTRADOR", "PRESIDENTE", "SECRETARIO", "SECRETARIA")) {
            HttpRequest req = HttpRequest.newBuilder(URI.create(url("/api/reuniones")))
                .header("Content-Type", "application/json")
                .header("X-Role", role)
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
            HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
            assertEquals(201, res.statusCode(), "Role " + role + " should be authorized");
        }
    }

    @Test
    void updateRechazaRolNoAutorizadoCon403() throws Exception {
        Reunion r = new Reunion(1, "Original", LocalDateTime.now().plusDays(2), "Lugar", Reunion.Tipo.ORDINARIA, Reunion.Estado.PROGRAMADA);
        reunionDAO.save(r);

        String json = """
            {"titulo":"Cambiado","fechaHora":"2026-10-15 14:00:00","lugar":"Lugar","tipo":"ORDINARIA"}
            """;
        HttpRequest req = HttpRequest.newBuilder(URI.create(url("/api/reuniones/1")))
            .header("Content-Type", "application/json")
            .header("X-Role", "MIEMBRO")
            .PUT(HttpRequest.BodyPublishers.ofString(json))
            .build();
        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(403, res.statusCode());
    }

    @Test
    void cancelarRechazaRolNoAutorizadoCon403() throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(url("/api/reuniones/1/cancelar")))
            .header("X-Role", "VOCAL")
            .method("PATCH", HttpRequest.BodyPublishers.noBody())
            .build();
        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(403, res.statusCode());
    }

    @Test
    void deleteRechazaRolNoAutorizadoCon403() throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(url("/api/reuniones/1")))
            .header("X-Role", "MIEMBRO")
            .DELETE()
            .build();
        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(403, res.statusCode());
    }

    @Test
    void deleteRetorna409CuandoTieneAsistencias() throws Exception {
        Reunion r = new Reunion(5, "Reunión", LocalDateTime.now().plusDays(1), "Cancha", Reunion.Tipo.ORDINARIA, Reunion.Estado.PROGRAMADA);
        reunionDAO.save(r);
        asistenciaDAO.save(new Asistencia(1L, 5, 10, true, "Presente"));

        HttpRequest req = HttpRequest.newBuilder(URI.create(url("/api/reuniones/5")))
            .header("X-Role", "ADMIN")
            .DELETE()
            .build();
        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(409, res.statusCode());
        assertTrue(res.body().contains("asistencia"));
    }

    @Test
    void deleteRetorna404CuandoNoExiste() throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(url("/api/reuniones/999")))
            .header("X-Role", "ADMIN")
            .DELETE()
            .build();
        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(404, res.statusCode());
    }

    private static final class MemoryReunionDAO extends ReunionDAO {
        private final List<Reunion> store = new ArrayList<>();
        private int seq = 1;

        @Override
        public Reunion save(Reunion entity) {
            if (entity.getIdReunion() == null) entity.setIdReunion(seq++);
            store.add(entity);
            return entity;
        }

        @Override
        public Optional<Reunion> findById(Integer id) {
            return store.stream().filter(r -> r.getIdReunion().equals(id)).findFirst();
        }

        @Override
        public Reunion update(Reunion entity) {
            store.removeIf(r -> r.getIdReunion().equals(entity.getIdReunion()));
            store.add(entity);
            return entity;
        }

        @Override
        public boolean delete(Integer id) {
            return store.removeIf(r -> r.getIdReunion().equals(id));
        }
    }

    private static final class MemoryAsistenciaDAO extends AsistenciaDAO {
        private final List<Asistencia> store = new ArrayList<>();

        @Override
        public Asistencia save(Asistencia entity) {
            store.add(entity);
            return entity;
        }

        @Override
        public int countByReunion(Integer idReunion) {
            return (int) store.stream().filter(a -> a.getIdReunion().equals(idReunion)).count();
        }
    }
}
