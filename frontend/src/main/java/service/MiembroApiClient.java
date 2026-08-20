package service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import models.MiembroModel;
import security.SessionManager;

public final class MiembroApiClient {
    private final QaApiConfig config;
    private final HttpClient client;
    private final ObjectMapper json;

    public MiembroApiClient() { this(QaApiConfig.load()); }
    MiembroApiClient(QaApiConfig config) {
        this.config = config;
        client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        json = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public List<MiembroModel> findAll() throws IOException, InterruptedException {
        HttpResponse<String> response = client.send(request("/api/miembros").GET().build(), HttpResponse.BodyHandlers.ofString());
        ensure(response, 200);
        return Arrays.stream(json.readValue(response.body(), MiembroResponse[].class)).map(MiembroResponse::toModel).toList();
    }

    public CreateMemberResult create(CreateMemberRequest value) throws IOException, InterruptedException {
        HttpRequest request = request("/api/miembros").header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(value))).build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        ensure(response, 201);
        CreateMemberResponse created = json.readValue(response.body(), CreateMemberResponse.class);
        return new CreateMemberResult(created.member().toModel(), created.username(), created.temporaryPassword());
    }

    private HttpRequest.Builder request(String path) {
        return HttpRequest.newBuilder(URI.create(config.baseUrl() + path)).timeout(Duration.ofSeconds(20))
            .header("Authorization", "Bearer " + SessionManager.getInstance().requireToken())
            .header("Accept", "application/json").header("ngrok-skip-browser-warning", "1");
    }
    private void ensure(HttpResponse<String> response, int expected) throws IOException {
        if (response.statusCode() == expected) return;
        String message = "HTTP " + response.statusCode();
        try { message = json.readTree(response.body()).path("error").asText(message); } catch (Exception ignored) { }
        throw new IOException(message);
    }

    public record CreateMemberRequest(String documento, String tipoDocumento, String paisOrigen,
        String nombres, String apellidos, String telefono, String correo, Integer idVivienda) { }
    public record CreateMemberResult(MiembroModel member, String username, String temporaryPassword) { }
    private record CreateMemberResponse(MiembroResponse member, String username, String temporaryPassword) { }
    private record MiembroResponse(Integer id, String dui, String tipoDocumento, String paisOrigen,
        Integer idVivienda, String nombres, String apellidos, String telefono, String correo,
        String direccion, String fechaIngreso, String estado) {
        MiembroModel toModel() { return new MiembroModel(id, dui, tipoDocumento, paisOrigen, idVivienda,
            nombres, apellidos, telefono, correo, direccion, fechaIngreso, estado); }
    }
}
