package service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import models.PeriodoDirectivaModel;
import security.SessionManager;

public final class PeriodoApiClient {
    private final QaApiConfig config;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public PeriodoApiClient() {
        this(QaApiConfig.load());
    }

    PeriodoApiClient(QaApiConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        this.objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public List<PeriodoDirectivaModel> findAll() throws IOException, InterruptedException {
        return findFiltered(null, null);
    }

    public List<PeriodoDirectivaModel> findFiltered(String estado, String busqueda) throws IOException, InterruptedException {
        StringBuilder query = new StringBuilder("/api/periodos?");
        if (estado != null && !estado.isBlank() && !"TODOS".equalsIgnoreCase(estado)) {
            query.append("&estado=").append(encode(estado.trim()));
        }
        if (busqueda != null && !busqueda.isBlank()) {
            query.append("&busqueda=").append(encode(busqueda.trim()));
        }

        HttpRequest request = requestBuilder(query.toString()).GET().build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException(error(response));
        }

        PeriodoResponse[] values = objectMapper.readValue(response.body(), PeriodoResponse[].class);
        return Arrays.stream(values).map(PeriodoResponse::toModel).toList();
    }

    public PeriodoDirectivaModel findActivo() throws IOException, InterruptedException {
        HttpRequest request = requestBuilder("/api/periodos/activo").GET().build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 404) {
            return null;
        }
        if (response.statusCode() != 200) {
            throw new IOException(error(response));
        }

        return objectMapper.readValue(response.body(), PeriodoResponse.class).toModel();
    }

    public PeriodoDirectivaModel findById(Integer id) throws IOException, InterruptedException {
        HttpRequest request = requestBuilder("/api/periodos/" + id).GET().build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException(error(response));
        }

        return objectMapper.readValue(response.body(), PeriodoResponse.class).toModel();
    }

    public PeriodoDirectivaModel create(PeriodoDirectivaModel model) throws IOException, InterruptedException {
        String body = objectMapper.writeValueAsString(Map.of(
            "nombre", model.getNombre(),
            "fechaInicio", model.getFechaInicio(),
            "fechaFin", model.getFechaFin(),
            "estado", model.getEstado() != null ? model.getEstado() : "PLANIFICADO"
        ));

        HttpRequest request = requestBuilder("/api/periodos")
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 201) {
            throw new IOException(error(response));
        }
        return objectMapper.readValue(response.body(), PeriodoResponse.class).toModel();
    }

    public PeriodoDirectivaModel update(Integer id, PeriodoDirectivaModel model) throws IOException, InterruptedException {
        String body = objectMapper.writeValueAsString(Map.of(
            "nombre", model.getNombre(),
            "fechaInicio", model.getFechaInicio(),
            "fechaFin", model.getFechaFin(),
            "estado", model.getEstado() != null ? model.getEstado() : "PLANIFICADO"
        ));

        HttpRequest request = requestBuilder("/api/periodos/" + id)
            .header("Content-Type", "application/json")
            .PUT(HttpRequest.BodyPublishers.ofString(body))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException(error(response));
        }
        return objectMapper.readValue(response.body(), PeriodoResponse.class).toModel();
    }

    public PeriodoDirectivaModel activar(Integer id) throws IOException, InterruptedException {
        HttpRequest request = requestBuilder("/api/periodos/" + id + "/activar")
            .method("PATCH", HttpRequest.BodyPublishers.noBody())
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException(error(response));
        }
        return objectMapper.readValue(response.body(), PeriodoResponse.class).toModel();
    }

    public PeriodoDirectivaModel finalizar(Integer id) throws IOException, InterruptedException {
        HttpRequest request = requestBuilder("/api/periodos/" + id + "/finalizar")
            .method("PATCH", HttpRequest.BodyPublishers.noBody())
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException(error(response));
        }
        return objectMapper.readValue(response.body(), PeriodoResponse.class).toModel();
    }

    public void delete(Integer id) throws IOException, InterruptedException {
        HttpRequest request = requestBuilder("/api/periodos/" + id).DELETE().build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException(error(response));
        }
    }

    private HttpRequest.Builder requestBuilder(String path) {
        return HttpRequest.newBuilder(URI.create(config.baseUrl() + path))
            .timeout(Duration.ofSeconds(20))
            .header("Authorization", "Bearer " + SessionManager.getInstance().requireToken())
            .header("Accept", "application/json")
            .header("ngrok-skip-browser-warning", "1");
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String error(HttpResponse<String> response) {
        try {
            return objectMapper.readTree(response.body()).path("error").asText("HTTP " + response.statusCode());
        } catch (Exception ignored) {
            return "HTTP " + response.statusCode();
        }
    }

    private record PeriodoResponse(
        Integer id, String nombre, String fechaInicio, String fechaFin, String estado, boolean vigente
    ) {
        private PeriodoDirectivaModel toModel() {
            return new PeriodoDirectivaModel(id, nombre, fechaInicio, fechaFin, estado, vigente);
        }
    }
}
