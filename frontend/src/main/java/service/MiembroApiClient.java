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
