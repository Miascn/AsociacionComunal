package sv.asociacion.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

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
import sv.asociacion.domain.dto.AportacionPageResponse;
import sv.asociacion.domain.dto.AportacionResponse;
import sv.asociacion.domain.dto.AsignacionCargoResponse;
import sv.asociacion.domain.dto.AsistenciaResponse;
import sv.asociacion.domain.dto.BitacoraResponse;
import sv.asociacion.domain.dto.CargoResponse;
import sv.asociacion.domain.dto.EstadoParticipacionResponse;
import sv.asociacion.domain.dto.OpcionVotacionResponse;
import sv.asociacion.domain.dto.PeriodoResponse;
import sv.asociacion.domain.dto.ProyectoResponse;
import sv.asociacion.domain.dto.ReunionResponse;
import sv.asociacion.domain.dto.RolResponse;
import sv.asociacion.domain.dto.UsuarioResponse;
import sv.asociacion.domain.dto.VotacionResponse;
import sv.asociacion.domain.entity.Proyecto;
import sv.asociacion.service.*;

/**
 * Regresión de SCRUM-330 — parámetros opcionales incompatibles con Javalin 7.
 *
 * <p>Antes de la corrección, {@code Validator.getOrDefault(null)} violaba el contrato
 * non-null de Javalin 7.2.2 y lanzaba {@code NullPointerException} <b>siempre</b>, incluso
 * con un valor válido. El manejador global de {@code ApiServer:225} la convertía en un
 * HTTP 500, de modo que toda ruta con parámetro de ruta o de consulta era inservible.
 *
 * <p>Se cubre una ruta por cada uno de los 13 controladores afectados, más los 4
 * parámetros de consulta opcionales, ausentes y presentes.
 *
 * <p>Los servicios son dobles de prueba que devuelven vacío o {@code null}: lo que se
 * verifica es la <b>extracción del parámetro</b>, que ocurre antes de cualquier lógica de
 * negocio. Un identificador válido debe llegar al servicio —y por tanto producir 404 o
 * 200, nunca 500—; uno no convertible debe cortarse en 400 sin alcanzarlo.
 */
class ControllerPathParamRegressionTest {

    /** etiqueta · ruta con identificador no convertible · ruta con identificador válido */
    private static final String[][] RUTAS = {
        {"AportacionController",      "/api/aportaciones/abc",                  "/api/aportaciones/9"},
        {"AsignacionCargoController", "/api/asignaciones/abc",                  "/api/asignaciones/9"},
        {"AsistenciaController",      "/api/reuniones/abc/asistencias",         "/api/reuniones/9/asistencias"},
        {"BitacoraController",        "/api/bitacoras/abc",                     "/api/bitacoras/9"},
        {"CargoController",           "/api/cargos/abc",                        "/api/cargos/9"},
        {"OpcionVotacionController",  "/api/opciones/abc",                      "/api/opciones/9"},
        {"PeriodoController",         "/api/periodos/abc",                      "/api/periodos/9"},
        {"ProyectoController",        "/api/proyectos/abc",                     "/api/proyectos/9"},
        {"ReunionController",         "/api/reuniones/abc",                     "/api/reuniones/9"},
        {"RolController",             "/api/roles/abc",                         "/api/roles/9"},
        {"UsuarioController",         "/api/usuarios/abc",                      "/api/usuarios/9"},
        {"VotacionController",        "/api/votaciones/abc",                    "/api/votaciones/9"},
        {"VotoController",            "/api/votaciones/abc/participacion?miembroId=1",
                                      "/api/votaciones/9/participacion?miembroId=1"},
    };

    private Javalin app;
    private HttpClient client;
    private StubAportacionService aportaciones;
    private StubCargoService cargos;
    private StubProyectoService proyectos;

    @BeforeEach
    void startServer() {
        aportaciones = new StubAportacionService();
        cargos = new StubCargoService();
        proyectos = new StubProyectoService();

        AportacionController cAportacion = new AportacionController(aportaciones);
        AsignacionCargoController cAsignacion = new AsignacionCargoController(new StubAsignacionCargoService());
        AsistenciaController cAsistencia = new AsistenciaController(new StubAsistenciaService());
        BitacoraController cBitacora = new BitacoraController(new StubBitacoraService());
        CargoController cCargo = new CargoController(cargos);
        OpcionVotacionController cOpcion = new OpcionVotacionController(new StubOpcionVotacionService());
        PeriodoController cPeriodo = new PeriodoController(new StubPeriodoService());
        ProyectoController cProyecto = new ProyectoController(proyectos);
        ReunionController cReunion = new ReunionController(new StubReunionService());
        RolController cRol = new RolController(new StubRolService());
        UsuarioController cUsuario = new UsuarioController(new StubUsuarioService());
        VotacionController cVotacion = new VotacionController(new StubVotacionService());
        VotoController cVoto = new VotoController(new StubVotoService());

        app = Javalin.create(config -> {
            // Replica lo que hace JwtAuthMiddleware:41. Se usa ADMIN para que ninguna
            // guarda canManage/canView interfiera: lo que se prueba aquí es la
            // extracción de parámetros, no la autorización.
            config.routes.before("/api/*", context -> context.attribute("role", "ADMIN"));

            config.routes.get("/api/aportaciones", cAportacion::getAll);
            config.routes.get("/api/aportaciones/{id}", cAportacion::getById);
            config.routes.get("/api/asignaciones/{id}", cAsignacion::getById);
            config.routes.get("/api/reuniones/{idReunion}/asistencias", cAsistencia::getByReunion);
            config.routes.get("/api/bitacoras/{id}", cBitacora::getById);
            config.routes.get("/api/cargos", cCargo::getAll);
            config.routes.get("/api/cargos/{id}", cCargo::getById);
            config.routes.get("/api/opciones/{id}", cOpcion::getById);
            config.routes.get("/api/periodos/{id}", cPeriodo::getById);
            config.routes.get("/api/proyectos", cProyecto::getAll);
            config.routes.get("/api/proyectos/{id}", cProyecto::getById);
            config.routes.get("/api/reuniones/{id}", cReunion::getById);
            config.routes.get("/api/roles/{id}", cRol::getById);
            config.routes.get("/api/usuarios/{id}", cUsuario::getById);
            config.routes.get("/api/votaciones/{id}", cVotacion::getById);
            config.routes.get("/api/votaciones/{idVotacion}/participacion", cVoto::verificarParticipacion);
        }).start("127.0.0.1", 0);

        client = HttpClient.newHttpClient();
    }

    @AfterEach
    void stopServer() {
        if (app != null) app.stop();
    }

    @Test
    void pathParamValidoNoProduce500() throws Exception {
        for (String[] ruta : RUTAS) {
            int status = get(ruta[2]).statusCode();
            assertNotEquals(500, status,
                ruta[0] + " devolvió 500 con un identificador válido en " + ruta[2]);
        }
    }

    @Test
    void pathParamNoConvertibleDevuelve400() throws Exception {
        for (String[] ruta : RUTAS) {
            assertEquals(400, get(ruta[1]).statusCode(),
                ruta[0] + " debería responder 400 ante un identificador no convertible en " + ruta[1]);
        }
    }

    @Test
    void queryParamsOpcionalesAusentesNoProducenError() throws Exception {
        // Los 4 parámetros de consulta opcionales: ausentes significan "sin filtro".
        for (String ruta : List.of("/api/aportaciones", "/api/cargos", "/api/proyectos")) {
            int status = get(ruta).statusCode();
            assertNotEquals(500, status, ruta + " devolvió 500 sin parámetros de consulta");
            assertNotEquals(400, status, ruta + " devolvió 400 sin parámetros de consulta");
        }

        assertNull(aportaciones.idMiembroRecibido, "idMiembro ausente debe llegar como null");
        assertNull(aportaciones.idProyectoRecibido, "idProyecto ausente debe llegar como null");
        assertNull(cargos.activoRecibido, "activo ausente debe llegar como null");
        assertNull(proyectos.creadoPorRecibido, "creadoPor ausente debe llegar como null");
    }

    @Test
    void queryParamsOpcionalesPresentesSeParsean() throws Exception {
        assertNotEquals(500, get("/api/aportaciones?idMiembro=7&idProyecto=11").statusCode());
        assertEquals(7, aportaciones.idMiembroRecibido);
        assertEquals(11, aportaciones.idProyectoRecibido);

        assertNotEquals(500, get("/api/cargos?activo=true").statusCode());
        assertEquals(Boolean.TRUE, cargos.activoRecibido);

        assertNotEquals(500, get("/api/proyectos?creadoPor=3").statusCode());
        assertEquals(3, proyectos.creadoPorRecibido);
    }

    @Test
    void queryParamNumericoNoConvertibleDevuelve400() throws Exception {
        // Contraparte para los filtros numéricos: presentes pero inservibles se
        // rechazan con 400, no se convierten en 500.
        //
        // Solo se comprueban los de tipo numérico. CargoController.activo es Boolean
        // y conserva su comportamiento preexistente —Boolean.parseBoolean devuelve
        // false ante cualquier texto no reconocido, sin error—, declarado fuera del
        // alcance de SCRUM-330.
        assertEquals(400, get("/api/aportaciones?idMiembro=abc").statusCode());
        assertEquals(400, get("/api/proyectos?creadoPor=abc").statusCode());
    }

    private HttpResponse<String> get(String path) throws Exception {
        return client.send(
            HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + app.port() + path)).GET().build(),
            HttpResponse.BodyHandlers.ofString());
    }

    // ------------------------------------------------------------------
    // Dobles de prueba. Cada uno sobrescribe solo el método que usa la ruta
    // elegida, de modo que los DAO del constructor nunca se dereferencian.
    // ------------------------------------------------------------------

    private static final class StubAportacionService extends AportacionService {
        Integer idMiembroRecibido;
        Integer idProyectoRecibido;

        StubAportacionService() { super(null, null, null); }

        @Override
        public AportacionResponse findById(Long id) { return null; }

        @Override
        public AportacionPageResponse findPage(Integer idMiembro, Integer idProyecto, String periodo,
                                               String desde, String hasta, String metodo, String estado,
                                               String busqueda, int page, int size) {
            this.idMiembroRecibido = idMiembro;
            this.idProyectoRecibido = idProyecto;
            return AportacionPageResponse.of(List.of(), 0, BigDecimal.ZERO, page, size);
        }
    }

    private static final class StubAsignacionCargoService extends AsignacionCargoService {
        StubAsignacionCargoService() { super(null, null, null, null); }

        @Override
        public AsignacionCargoResponse findById(Integer id) { return null; }
    }

    private static final class StubAsistenciaService extends AsistenciaService {
        StubAsistenciaService() { super(null, null, null); }

        @Override
        public List<AsistenciaResponse> getByReunion(Integer idReunion) { return List.of(); }
    }

    private static final class StubBitacoraService extends BitacoraService {
        StubBitacoraService() { super(null); }

        @Override
        public BitacoraResponse findById(Long id) { return null; }
    }

    private static final class StubCargoService extends CargoService {
        Boolean activoRecibido;

        StubCargoService() { super(null); }

        @Override
        public CargoResponse findById(Integer id) { return null; }

        @Override
        public List<CargoResponse> findFiltered(Boolean activo, String busqueda) {
            this.activoRecibido = activo;
            return List.of();
        }
    }

    private static final class StubOpcionVotacionService extends OpcionVotacionService {
        StubOpcionVotacionService() { super(null, null, null); }

        @Override
        public OpcionVotacionResponse findById(Integer id) { return null; }
    }

    private static final class StubPeriodoService extends PeriodoService {
        StubPeriodoService() { super(null); }

        @Override
        public PeriodoResponse findById(Integer id) { return null; }
    }

    private static final class StubProyectoService extends ProyectoService {
        Integer creadoPorRecibido;

        StubProyectoService() { super(null); }

        @Override
        public ProyectoResponse findById(Integer id) { return null; }

        @Override
        public List<ProyectoResponse> findFiltered(Proyecto.Estado estado, Integer creadoPor, String busqueda) {
            this.creadoPorRecibido = creadoPor;
            return List.of();
        }
    }

    private static final class StubReunionService extends ReunionService {
        StubReunionService() { super(null, null); }

        // El servicio real lanza IllegalArgumentException cuando no encuentra la
        // reunión y el controlador la traduce a 404; no devuelve null. El doble
        // reproduce ese contrato.
        @Override
        public ReunionResponse getById(Integer id) {
            throw new IllegalArgumentException("Reunión no encontrada con ID: " + id);
        }
    }

    private static final class StubRolService extends RolService {
        StubRolService() { super(null); }

        @Override
        public RolResponse findById(Integer id) { return null; }
    }

    private static final class StubUsuarioService extends UsuarioService {
        StubUsuarioService() { super(null, null, null); }

        @Override
        public UsuarioResponse findById(Integer id) { return null; }
    }

    private static final class StubVotacionService extends VotacionService {
        StubVotacionService() { super(null, null, null, null); }

        @Override
        public VotacionResponse findById(Integer id) { return null; }
    }

    private static final class StubVotoService extends VotoService {
        StubVotoService() { super(null, null, null, null); }

        @Override
        public EstadoParticipacionResponse verificarParticipacion(Integer idVotacion, Integer idMiembro) {
            return new EstadoParticipacionResponse(idVotacion, idMiembro, false, null);
        }
    }
}
