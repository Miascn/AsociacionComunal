package service;

import com.fasterxml.jackson.databind.ObjectMapper;
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
        this.objectMapper = new ObjectMapper();
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

    public Miembro create(CreateMemberRequest value) throws IOException, InterruptedException {
        HttpRequest request = authorizedRequest("/api/miembros")
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(value)))
            .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 201) {
            throw new IOException(response.statusCode() == 409
                ? "Ya existe un miembro con ese DUI."
                : "No fue posible registrar el miembro (HTTP " + response.statusCode() + ").");
        }
        return objectMapper.readValue(response.body(), MiembroResponse.class).toEntity();
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
        String dui,
        String nombres,
        String apellidos,
        String telefono,
        String correo,
        String direccion
    ) {}

    private record MiembroResponse(
        Integer id, String dui, String nombres, String apellidos, String telefono,
        String correo, String direccion, String fechaIngreso, String estado
    ) {
        private Miembro toEntity() {
            return new Miembro(
                id, dui, nombres, apellidos, telefono, correo, direccion,
                fechaIngreso == null ? null : LocalDate.parse(fechaIngreso),
                estado == null ? null : Miembro.Estado.valueOf(estado)
            );
        }
    }
}
