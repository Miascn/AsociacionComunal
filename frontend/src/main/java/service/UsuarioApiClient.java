package service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import models.UsuarioModel;
import security.SessionManager;

public final class UsuarioApiClient {
    private final QaApiConfig config = QaApiConfig.load();
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final ObjectMapper json = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public List<UsuarioModel> findAll() throws IOException, InterruptedException {
        return findAll(null);
    }

    public List<UsuarioModel> findAll(String tipo) throws IOException, InterruptedException {
        String path = "/api/usuarios" + (tipo != null && !tipo.isBlank() ? "?tipo=" + tipo.trim() : "");
        HttpResponse<String> response = send(request(path).GET().build());
        ensure(response, 200);
        return Arrays.asList(json.readValue(response.body(), UsuarioModel[].class));
    }

    public UsuarioModel create(UsuarioRequest value) throws IOException, InterruptedException {
        CreateRequest body = new CreateRequest(value.nombreUsuario(), value.clave(), value.idRol(), value.idMiembro());
        HttpResponse<String> response = send(request("/api/usuarios").header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body))).build());
        ensure(response, 201);
        return json.readValue(response.body(), UsuarioModel.class);
    }

    public UsuarioModel update(int id, UsuarioRequest value) throws IOException, InterruptedException {
        HttpResponse<String> response = send(request("/api/usuarios/" + id).header("Content-Type", "application/json")
            .PUT(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(value))).build());
        ensure(response, 200);
        return json.readValue(response.body(), UsuarioModel.class);
    }

    public UsuarioModel changeState(int id, String state) throws IOException, InterruptedException {
        String body = json.writeValueAsString(new StateRequest(state));
        HttpResponse<String> response = send(request("/api/usuarios/" + id + "/estado").header("Content-Type", "application/json")
            .method("PATCH", HttpRequest.BodyPublishers.ofString(body)).build());
        ensure(response, 200);
        return json.readValue(response.body(), UsuarioModel.class);
    }

    public ResetPasswordResult resetPassword(int id) throws IOException, InterruptedException {
        HttpResponse<String> response = send(request("/api/usuarios/" + id + "/restablecer-clave")
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.noBody()).build());
        if (response.statusCode() == 404) {
            response = send(request("/api/usuarios/" + id + "/reset-password")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.noBody()).build());
        }
        ensure(response, 200);
        return json.readValue(response.body(), ResetPasswordResult.class);
    }

    private HttpRequest.Builder request(String path) {
        return HttpRequest.newBuilder(URI.create(config.baseUrl() + path)).timeout(Duration.ofSeconds(20))
            .header("Authorization", "Bearer " + SessionManager.getInstance().requireToken())
            .header("Accept", "application/json").header("ngrok-skip-browser-warning", "1");
    }

    private HttpResponse<String> send(HttpRequest request) throws IOException, InterruptedException {
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private void ensure(HttpResponse<String> response, int expected) throws IOException {
        if (response.statusCode() == expected) return;
        String message = "HTTP " + response.statusCode();
        try { message = json.readTree(response.body()).path("error").asText(message); } catch (Exception ignored) { }
        throw new IOException(message);
    }

    public record UsuarioRequest(String nombreUsuario, String clave, Integer idRol, Integer idMiembro, String estado) { }
    public record ResetPasswordResult(Integer idUsuario, String nombreUsuario, String temporaryPassword) { }
    private record CreateRequest(String nombreUsuario, String clave, Integer idRol, Integer idMiembro) { }
    private record StateRequest(String estado) { }
}
