package sv.asociacion.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sv.asociacion.backend.dao.MiembroDAO;
import sv.asociacion.backend.entity.Miembro;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class MemberRoutesTest {
    private static final String SECRET = "01234567890123456789012345678901";
    private final ObjectMapper json = new ObjectMapper();
    private Javalin app;
    private HttpClient client;
    private MemoryMiembroDAO dao;

    @BeforeEach
    void startServer() {
        dao = new MemoryMiembroDAO(List.of(
            member(1, "012345678", "Ana", Miembro.Estado.ACTIVO),
            member(2, "987654321", "Luis", Miembro.Estado.ACTIVO)
        ));
        app = Javalin.create(config -> ApiServer.registerMemberRoutes(config.routes, dao, SECRET))
            .start("127.0.0.1", 0);
        client = HttpClient.newHttpClient();
    }

    @AfterEach
    void stopServer() {
        if (app != null) app.stop();
    }

    @Test
    void protegeTodoElCrudYConsultaDetalle() throws Exception {
        assertEquals(401, request("GET", "/api/miembros/1", null, null).statusCode());
        HttpResponse<String> found = request("GET", "/api/miembros/1", null, SECRET);
        assertEquals(200, found.statusCode());
        assertEquals(1, json.readTree(found.body()).path("id").asInt());
        assertEquals(404, request("GET", "/api/miembros/99", null, SECRET).statusCode());
    }

    @Test
    void actualizaSinCambiarIdentificadorYDetectaDocumentoDuplicado() throws Exception {
        String update = requestBody("111111111", "Ana María");
        HttpResponse<String> response = request("PUT", "/api/miembros/1", update, SECRET);
        assertEquals(200, response.statusCode());
        assertEquals(1, json.readTree(response.body()).path("id").asInt());
        assertEquals("Ana María", dao.findById(1).orElseThrow().getNombres());

        HttpResponse<String> duplicate = request("PUT", "/api/miembros/1", requestBody("987654321", "Ana"), SECRET);
        assertEquals(409, duplicate.statusCode());
        assertEquals(1, dao.findById(1).orElseThrow().getIdMiembro());
    }

    @Test
    void desactivaYReactivaSinEliminarRegistro() throws Exception {
        HttpResponse<String> inactive = request("PATCH", "/api/miembros/1/estado", "{\"estado\":\"INACTIVO\"}", SECRET);
        assertEquals(200, inactive.statusCode());
        assertEquals(Miembro.Estado.INACTIVO, dao.findById(1).orElseThrow().getEstado());

        HttpResponse<String> active = request("PATCH", "/api/miembros/1/estado", "{\"estado\":\"ACTIVO\"}", SECRET);
        assertEquals(200, active.statusCode());
        assertEquals(Miembro.Estado.ACTIVO, dao.findById(1).orElseThrow().getEstado());

        assertEquals(204, request("DELETE", "/api/miembros/1", null, SECRET).statusCode());
        assertTrue(dao.findById(1).isPresent());
        assertEquals(Miembro.Estado.INACTIVO, dao.findById(1).orElseThrow().getEstado());
    }

    @Test
    void validaDatosYEstadoSolicitado() throws Exception {
        assertEquals(400, request("PUT", "/api/miembros/1", requestBody("123", "Ana"), SECRET).statusCode());
        assertEquals(400, request("PATCH", "/api/miembros/1/estado", "{\"estado\":\"ELIMINADO\"}", SECRET).statusCode());
        assertEquals(404, request("PATCH", "/api/miembros/99/estado", "{\"estado\":\"ACTIVO\"}", SECRET).statusCode());
    }

    private HttpResponse<String> request(String method, String path, String body, String secret) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + app.port() + path));
        if (secret != null) builder.header("Authorization", "Bearer " + secret);
        if (body != null) builder.header("Content-Type", "application/json");
        builder.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private String requestBody(String document, String names) throws Exception {
        return json.writeValueAsString(Map.of(
            "documento", document,
            "tipoDocumento", "DUI",
            "nombres", names,
            "apellidos", "Pérez",
            "telefono", "7000-0000",
            "correo", "ana@example.com",
            "idVivienda", 1
        ));
    }

    private static Miembro member(int id, String document, String names, Miembro.Estado state) {
        return new Miembro(id, document, "DUI", null, 1, names, "Pérez", "7000-0000",
            "persona@example.com", null, LocalDate.of(2026, 1, 1), state);
    }

    private static final class MemoryMiembroDAO extends MiembroDAO {
        private final Map<Integer, Miembro> values = new LinkedHashMap<>();

        private MemoryMiembroDAO(List<Miembro> members) {
            members.forEach(member -> values.put(member.getIdMiembro(), member));
        }

        @Override public Optional<Miembro> findById(Integer id) { return Optional.ofNullable(values.get(id)); }
        @Override public List<Miembro> findAll() { return new ArrayList<>(values.values()); }
        @Override public boolean existsByDuiExcludingId(String dui, Integer id) {
            return values.values().stream().anyMatch(member -> !member.getIdMiembro().equals(id) && member.getDui().equals(dui));
        }
        @Override public Miembro update(Miembro member) {
            if (!values.containsKey(member.getIdMiembro())) return null;
            values.put(member.getIdMiembro(), member);
            return member;
        }
        @Override public boolean changeEstado(Integer id, Miembro.Estado estado) {
            Miembro member = values.get(id);
            if (member == null) return false;
            member.setEstado(estado);
            return true;
        }
    }
}
