package sv.asociacion.controller;

import io.javalin.Javalin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sv.asociacion.ApiServer;
import sv.asociacion.exception.ConexionBDException;
import sv.asociacion.exception.ConflictoException;
import sv.asociacion.exception.PersistenciaException;
import sv.asociacion.exception.RecursoNoEncontradoException;
import sv.asociacion.exception.ValidacionException;
import sv.asociacion.exception.VotoDuplicadoException;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Traduccion centralizada de excepciones a HTTP, sobre un Javalin real montado con
 * {@link ApiServer#configurarManejoErrores} (la misma configuracion de produccion).
 */
class ManejoErroresRoutesTest {

    private static final String SQL_INTERNO = "Table 'asociacion_comunal.miembro' doesn't exist";

    private Javalin app;
    private final HttpClient client = HttpClient.newHttpClient();

    @BeforeEach
    void iniciar() {
        app = Javalin.create(cfg -> {
            ApiServer.configurarJson(cfg);
            ApiServer.configurarManejoErrores(cfg);
            cfg.routes.get("/validacion", ctx -> { throw new ValidacionException("El DUI es obligatorio."); });
            cfg.routes.get("/no-encontrado", ctx -> { throw new RecursoNoEncontradoException("Reunión no encontrada."); });
            cfg.routes.get("/conflicto", ctx -> { throw new ConflictoException("La reunión está cancelada."); });
            cfg.routes.get("/voto-duplicado", ctx -> { throw new VotoDuplicadoException("Ya votó.", null); });
            cfg.routes.get("/iae", ctx -> { throw new IllegalArgumentException("Monto inválido."); });
            cfg.routes.get("/ise", ctx -> { throw new IllegalStateException("Período cerrado."); });
            cfg.routes.get("/persistencia", ctx -> {
                throw new PersistenciaException("No fue posible consultar los miembros.",
                    new SQLException(SQL_INTERNO, "42S02", 1146));
            });
            cfg.routes.get("/bd-caida", ctx -> {
                throw new ConexionBDException("No fue posible consultar.",
                    new SQLException("Communications link failure", "08S01"));
            });
            cfg.routes.get("/inesperado", ctx -> { throw new NullPointerException("detalle interno"); });
        }).start("127.0.0.1", 0);
    }

    @AfterEach
    void detener() {
        app.stop();
    }

    @Test
    @DisplayName("Errores de negocio: 400, 404 y 409 con el mensaje para el usuario")
    void erroresDeNegocio() throws Exception {
        assertRespuesta("/validacion", 400, "El DUI es obligatorio.");
        assertRespuesta("/no-encontrado", 404, "Reunión no encontrada.");
        assertRespuesta("/conflicto", 409, "La reunión está cancelada.");
        assertRespuesta("/voto-duplicado", 409, "Ya votó.");
    }

    @Test
    @DisplayName("Las excepciones estandar heredadas conservan su significado: IAE 400, ISE 409")
    void excepcionesEstandar() throws Exception {
        assertRespuesta("/iae", 400, "Monto inválido.");
        assertRespuesta("/ise", 409, "Período cerrado.");
    }

    @Test
    @DisplayName("Un fallo de persistencia es 500 y no expone el SQL al cliente")
    void persistenciaEs500SinFiltrarSql() throws Exception {
        HttpResponse<String> res = get("/persistencia");
        assertEquals(500, res.statusCode());
        assertFalse(res.body().contains("asociacion_comunal"), res.body());
        assertFalse(res.body().contains("No fue posible consultar los miembros"),
            "el cliente recibe un texto generico, no el de la operacion interna");
    }

    @Test
    @DisplayName("La base de datos caida es 503, distinguible de un error del codigo")
    void baseCaidaEs503() throws Exception {
        HttpResponse<String> res = get("/bd-caida");
        assertEquals(503, res.statusCode());
        assertTrue(res.body().contains("no está disponible"), res.body());
        assertFalse(res.body().contains("Communications link failure"));
    }

    @Test
    @DisplayName("Lo no previsto sigue siendo 500 generico")
    void inesperadoEs500() throws Exception {
        HttpResponse<String> res = get("/inesperado");
        assertEquals(500, res.statusCode());
        assertFalse(res.body().contains("detalle interno"));
    }

    private void assertRespuesta(String ruta, int status, String mensaje) throws Exception {
        HttpResponse<String> res = get(ruta);
        assertEquals(status, res.statusCode(), ruta + " -> " + res.body());
        assertTrue(res.body().contains(mensaje), ruta + " -> " + res.body());
    }

    private HttpResponse<String> get(String ruta) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + app.port() + ruta)).GET().build();
        return client.send(req, HttpResponse.BodyHandlers.ofString());
    }
}
