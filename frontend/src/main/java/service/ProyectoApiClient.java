package service;

import com.fasterxml.jackson.databind.ObjectMapper;
import sv.asociacion.backend.entity.Proyecto;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

public final class ProyectoApiClient {
    private final QaApiConfig config;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ProyectoApiClient() {
        config = QaApiConfig.load();
        httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    public List<Proyecto> findAll() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(config.baseUrl() + "/api/proyectos"))
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
        ProyectoResponse[] values = objectMapper.readValue(response.body(), ProyectoResponse[].class);
        return Arrays.stream(values).map(ProyectoResponse::toEntity).toList();
    }

    private record ProyectoResponse(
        Integer id, String nombre, String descripcion, java.math.BigDecimal presupuesto,
        String fechaCreacion, String estado
    ) {
        private Proyecto toEntity() {
            return new Proyecto(
                id, null, nombre, descripcion, presupuesto,
                fechaCreacion == null ? null : LocalDate.parse(fechaCreacion),
                estado == null ? null : Proyecto.Estado.valueOf(estado)
            );
        }
    }
}
