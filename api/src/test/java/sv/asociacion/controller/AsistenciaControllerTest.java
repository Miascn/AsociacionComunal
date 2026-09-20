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
import sv.asociacion.dao.MiembroDAO;
import sv.asociacion.dao.ReunionDAO;
import sv.asociacion.domain.entity.Asistencia;
import sv.asociacion.domain.entity.Miembro;
import sv.asociacion.domain.entity.Reunion;
import sv.asociacion.service.AsistenciaService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AsistenciaControllerTest {
    private Javalin app;
    private HttpClient client;
    private MemoryAsistenciaDAO asistenciaDAO;
    private MemoryReunionDAO reunionDAO;
    private MemoryMiembroDAO miembroDAO;

    @BeforeEach
    void setUp() {
        asistenciaDAO = new MemoryAsistenciaDAO();
        reunionDAO = new MemoryReunionDAO();
        miembroDAO = new MemoryMiembroDAO();
        AsistenciaService service = new AsistenciaService(asistenciaDAO, reunionDAO, miembroDAO);
        AsistenciaController controller = new AsistenciaController(service);

        // Populate sample entities
        Reunion rProgramada = new Reunion(1, "Asamblea General", LocalDateTime.now().plusDays(2), "Cancha", Reunion.Tipo.ORDINARIA, Reunion.Estado.PROGRAMADA);
        reunionDAO.save(rProgramada);

        Reunion rRealizada = new Reunion(2, "Asamblea Pasada", LocalDateTime.now().minusDays(1), "Cancha", Reunion.Tipo.ORDINARIA, Reunion.Estado.REALIZADA);
        reunionDAO.save(rRealizada);

        Miembro m1 = new Miembro();
        m1.setIdMiembro(10);
        m1.setNombres("Carlos");
        m1.setApellidos("Lopez");
        m1.setEstado(Miembro.Estado.ACTIVO);
        miembroDAO.save(m1);

        app = Javalin.create(config -> {
            sv.asociacion.ApiServer.configurarJson(config);
            sv.asociacion.ApiServer.configurarManejoErrores(config);
            config.routes.before(ctx -> {
                ctx.body();
                String role = ctx.header("X-Role");
                if (role != null) {
                    ctx.attribute("role", role);
                }
            });
            config.routes.get("/api/reuniones/{idReunion}/asistencias", controller::getByReunion);
            config.routes.post("/api/reuniones/{idReunion}/asistencias/convocar", controller::convocar);
            config.routes.post("/api/reuniones/{idReunion}/asistencias", controller::registrarOActualizar);
            config.routes.patch("/api/asistencias/{id}", controller::toggle);
            config.routes.delete("/api/asistencias/{id}", controller::delete);
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
    void getByReunionRetorna404CuandoReunionNoExiste() throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(url("/api/reuniones/999/asistencias"))).GET().build();
        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(404, res.statusCode());
        assertTrue(res.body().contains("no encontrada"));
    }

    @Test
    void convocarRechazaRolNoAutorizadoCon403() throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(url("/api/reuniones/1/asistencias/convocar")))
            .header("X-Role", "MIEMBRO")
            .POST(HttpRequest.BodyPublishers.noBody())
            .build();
        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(403, res.statusCode());
    }

    @Test
    void registrarOActualizarRechazaRolNoAutorizadoCon403() throws Exception {
        String json = """
            {"idMiembro":10,"asistio":true,"observacion":"Presente"}
            """;
        HttpRequest req = HttpRequest.newBuilder(URI.create(url("/api/reuniones/1/asistencias")))
            .header("Content-Type", "application/json")
            .header("X-Role", "MIEMBRO")
            .POST(HttpRequest.BodyPublishers.ofString(json))
            .build();
        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(403, res.statusCode());
    }

    @Test
    void registrarPermiteRolesAutorizadosCon200() throws Exception {
        String json = """
            {"idMiembro":10,"asistio":true,"observacion":"Presente"}
            """;
        for (String role : List.of("ADMIN", "ADMINISTRADOR", "PRESIDENTE", "SECRETARIO", "SECRETARIA")) {
            HttpRequest req = HttpRequest.newBuilder(URI.create(url("/api/reuniones/1/asistencias")))
                .header("Content-Type", "application/json")
                .header("X-Role", role)
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
            HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
            assertEquals(200, res.statusCode(), "Role " + role + " should be authorized");
        }
    }

    @Test
    void registrarRechazaMiembroInexistenteCon400() throws Exception {
        String json = """
            {"idMiembro":999,"asistio":true,"observacion":"Presente"}
            """;
        HttpRequest req = HttpRequest.newBuilder(URI.create(url("/api/reuniones/1/asistencias")))
            .header("Content-Type", "application/json")
            .header("X-Role", "ADMIN")
            .POST(HttpRequest.BodyPublishers.ofString(json))
            .build();
        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(400, res.statusCode());
        assertTrue(res.body().contains("Miembro no encontrado"));
    }

    @Test
    void toggleRechazaRolNoAutorizadoCon403() throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(url("/api/asistencias/1")))
            .header("Content-Type", "application/json")
            .header("X-Role", "MIEMBRO")
            .method("PATCH", HttpRequest.BodyPublishers.ofString("{\"asistio\":true}"))
            .build();
        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(403, res.statusCode());
    }

    @Test
    void deleteRechazaRolNoAutorizadoCon403() throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(url("/api/asistencias/1")))
            .header("X-Role", "MIEMBRO")
            .DELETE()
            .build();
        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(403, res.statusCode());
    }

    @Test
    void deleteRetorna409CuandoReunionEstaRealizada() throws Exception {
        Asistencia a = new Asistencia(100L, 2, 10, true, "Presente");
        asistenciaDAO.save(a);

        HttpRequest req = HttpRequest.newBuilder(URI.create(url("/api/asistencias/100")))
            .header("X-Role", "ADMIN")
            .DELETE()
            .build();
        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(409, res.statusCode());
        assertTrue(res.body().contains("REALIZADA"));
    }

    @Test
    void deleteRetorna404CuandoAsistenciaNoExiste() throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(url("/api/asistencias/999")))
            .header("X-Role", "ADMIN")
            .DELETE()
            .build();
        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(404, res.statusCode());
    }

    private static final class MemoryAsistenciaDAO extends AsistenciaDAO {
        private final List<Asistencia> store = new ArrayList<>();
        private long seq = 1L;

        @Override
        public Asistencia save(Asistencia entity) {
            if (entity.getIdAsistencia() == null) entity.setIdAsistencia(seq++);
            store.add(entity);
            return entity;
        }

        @Override
        public Asistencia update(Asistencia entity) {
            store.removeIf(a -> a.getIdAsistencia().equals(entity.getIdAsistencia()));
            store.add(entity);
            return entity;
        }

        @Override
        public Optional<Asistencia> findById(Long id) {
            return store.stream().filter(a -> a.getIdAsistencia().equals(id)).findFirst();
        }

        @Override
        public List<Asistencia> findByReunion(Integer idReunion) {
            return store.stream().filter(a -> a.getIdReunion().equals(idReunion)).toList();
        }

        @Override
        public Optional<Asistencia> findByReunionAndMiembro(Integer idReunion, Integer idMiembro) {
            return store.stream().filter(a -> a.getIdReunion().equals(idReunion) && a.getIdMiembro().equals(idMiembro)).findFirst();
        }

        @Override
        public boolean delete(Long id) {
            return store.removeIf(a -> a.getIdAsistencia().equals(id));
        }
    }

    private static final class MemoryReunionDAO extends ReunionDAO {
        private final List<Reunion> store = new ArrayList<>();

        @Override
        public Reunion save(Reunion entity) {
            store.add(entity);
            return entity;
        }

        @Override
        public Optional<Reunion> findById(Integer id) {
            return store.stream().filter(r -> r.getIdReunion().equals(id)).findFirst();
        }
    }

    private static final class MemoryMiembroDAO extends MiembroDAO {
        private final List<Miembro> store = new ArrayList<>();

        @Override
        public Miembro save(Miembro entity) {
            store.add(entity);
            return entity;
        }

        @Override
        public Optional<Miembro> findById(Integer id) {
            return store.stream().filter(m -> m.getIdMiembro().equals(id)).findFirst();
        }

        @Override
        public List<Miembro> findByEstado(Miembro.Estado estado) {
            return store.stream().filter(m -> m.getEstado() == estado).toList();
        }
    }
}
