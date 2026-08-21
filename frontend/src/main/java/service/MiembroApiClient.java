package service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import sv.asociacion.backend.entity.Miembro;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

public final class MiembroApiClient {
    private final QaApiConfig config;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public MiembroApiClient() {
        this(QaApiConfig.load());
    }

    MiembroApiClient(QaApiConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        this.objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public List<Miembro> findAll() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(config.baseUrl() + "/api/miembros"))
            .timeout(Duration.ofSeconds(20))
            .header("Authorization", "Bearer " + config.token())
            .header("Accept", "application/json")
            .header("ngrok-skip-browser-warning", "1")
            .GET()
            .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("La API respondio con estado " + response.statusCode() + ".");
        }
        MiembroResponse[] values = objectMapper.readValue(response.body(), MiembroResponse[].class);
        return Arrays.stream(values).map(MiembroResponse::toEntity).toList();
    }

    public Miembro findById(int id) throws IOException, InterruptedException {
        HttpResponse<String> response = send(authorizedRequest("/api/miembros/" + id).GET().build());
        if (response.statusCode() == 404) throw new IOException("El miembro ya no existe.");
        requireStatus(response, 200, "consultar el miembro");
        return objectMapper.readValue(response.body(), MiembroResponse.class).toEntity();
    }

    public CreateMemberResult create(CreateMemberRequest value) throws IOException, InterruptedException {
        HttpRequest request = authorizedRequest("/api/miembros")
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(value)))
            .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 201) {
            throw new IOException(response.statusCode() == 409
                ? "Ya existe un miembro con ese documento."
                : "No fue posible registrar el miembro (HTTP " + response.statusCode() + ").");
        }
        CreateMemberResponse created = objectMapper.readValue(response.body(), CreateMemberResponse.class);
        return new CreateMemberResult(created.member().toEntity(), created.username(), created.temporaryPassword());
    }

    public Miembro update(int id, CreateMemberRequest value) throws IOException, InterruptedException {
        HttpRequest request = authorizedRequest("/api/miembros/" + id)
            .header("Content-Type", "application/json")
            .PUT(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(value)))
            .build();
        HttpResponse<String> response = send(request);
        if (response.statusCode() == 404) throw new IOException("El miembro ya no existe.");
        if (response.statusCode() == 409) throw new IOException("Ya existe otro miembro con ese documento.");
        requireStatus(response, 200, "actualizar el miembro");
        return objectMapper.readValue(response.body(), MiembroResponse.class).toEntity();
    }

    public Miembro changeState(int id, Miembro.Estado estado) throws IOException, InterruptedException {
        String body = objectMapper.writeValueAsString(new MemberStateRequest(estado.name()));
        HttpRequest request = authorizedRequest("/api/miembros/" + id + "/estado")
            .header("Content-Type", "application/json")
            .method("PATCH", HttpRequest.BodyPublishers.ofString(body))
            .build();
        HttpResponse<String> response = send(request);
        if (response.statusCode() == 404) throw new IOException("El miembro ya no existe.");
        requireStatus(response, 200, "cambiar el estado del miembro");
        return objectMapper.readValue(response.body(), MiembroResponse.class).toEntity();
    }

    private HttpResponse<String> send(HttpRequest request) throws IOException, InterruptedException {
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private void requireStatus(HttpResponse<String> response, int expected, String operation) throws IOException {
        if (response.statusCode() != expected) {
            throw new IOException("No fue posible " + operation + " (HTTP " + response.statusCode() + ").");
        }
    }

    private HttpRequest.Builder authorizedRequest(String path) {
        return HttpRequest.newBuilder()
            .uri(URI.create(config.baseUrl() + path))
            .timeout(Duration.ofSeconds(20))
            .header("Authorization", "Bearer " + config.token())
            .header("Accept", "application/json")
            .header("ngrok-skip-browser-warning", "1");
    }

    public record CreateMemberRequest(
        String documento,
        String tipoDocumento,
        String paisOrigen,
        String nombres,
        String apellidos,
        String telefono,
        String correo,
        Integer idVivienda
    ) {}
    public record CreateMemberResult(Miembro member, String username, String temporaryPassword) {}
    private record CreateMemberResponse(MiembroResponse member, String username, String temporaryPassword) {}
    private record MemberStateRequest(String estado) {}

    private record MiembroResponse(
        Integer id, String dui, String tipoDocumento, String paisOrigen, Integer idVivienda, String nombres, String apellidos, String telefono,
        String correo, String direccion, String fechaIngreso, String estado
    ) {
        private Miembro toEntity() {
            return new Miembro(
                id, dui,
                tipoDocumento == null ? "DUI" : tipoDocumento,
                paisOrigen, idVivienda, nombres, apellidos, telefono, correo, direccion,
                fechaIngreso == null ? null : LocalDate.parse(fechaIngreso),
                estado == null ? null : Miembro.Estado.valueOf(estado)
            );
        }
    }
}
