package service;

import com.fasterxml.jackson.core.type.TypeReference;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import models.ReunionModel;
import security.SessionManager;

public class ReunionApiClient {
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final QaApiConfig config;

    public ReunionApiClient() {
        this(QaApiConfig.load());
    }

    public ReunionApiClient(QaApiConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
        this.objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public List<ReunionModel> getAll(String search, String tipo, String estado) throws IOException, InterruptedException {
        StringBuilder path = new StringBuilder("/api/reuniones?_t=" + System.currentTimeMillis());
        if (search != null && !search.isBlank()) {
            path.append("&search=").append(encode(search.trim()));
        }
        if (tipo != null && !tipo.isBlank() && !"TODOS".equalsIgnoreCase(tipo)) {
            path.append("&tipo=").append(encode(tipo.trim()));
        }
        if (estado != null && !estado.isBlank() && !"TODOS".equalsIgnoreCase(estado)) {
            path.append("&estado=").append(encode(estado.trim()));
        }

        HttpRequest request = requestBuilder(path.toString()).GET().build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException(error(response));
        }

        List<ReunionDto> dtos = objectMapper.readValue(response.body(), new TypeReference<>() {});
        return dtos.stream().map(ReunionDto::toModel).collect(Collectors.toList());
    }

    public ReunionModel getById(Integer id) throws IOException, InterruptedException {
        HttpRequest request = requestBuilder("/api/reuniones/" + id).GET().build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException(error(response));
        }

        return objectMapper.readValue(response.body(), ReunionDto.class).toModel();
    }

    public ReunionModel create(ReunionModel model) throws IOException, InterruptedException {
        Map<String, Object> map = toMap(model);
        HttpRequest request = requestBuilder("/api/reuniones")
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(map)))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 201) {
            throw new IOException(error(response));
        }

        return objectMapper.readValue(response.body(), ReunionDto.class).toModel();
    }

    public ReunionModel update(Integer id, ReunionModel model) throws IOException, InterruptedException {
        Map<String, Object> map = toMap(model);
        HttpRequest request = requestBuilder("/api/reuniones/" + id)
            .header("Content-Type", "application/json")
            .PUT(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(map)))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException(error(response));
        }

        return objectMapper.readValue(response.body(), ReunionDto.class).toModel();
    }

    public ReunionModel marcarRealizada(Integer id) throws IOException, InterruptedException {
        HttpRequest request = requestBuilder("/api/reuniones/" + id + "/realizada")
            .header("Content-Type", "application/json")
            .method("PATCH", HttpRequest.BodyPublishers.noBody())
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException(error(response));
        }

        return objectMapper.readValue(response.body(), ReunionDto.class).toModel();
    }

    public ReunionModel cancelar(Integer id) throws IOException, InterruptedException {
        HttpRequest request = requestBuilder("/api/reuniones/" + id + "/cancelar")
            .header("Content-Type", "application/json")
            .method("PATCH", HttpRequest.BodyPublishers.noBody())
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException(error(response));
        }

        return objectMapper.readValue(response.body(), ReunionDto.class).toModel();
    }

    public void delete(Integer id) throws IOException, InterruptedException {
        HttpRequest request = requestBuilder("/api/reuniones/" + id).DELETE().build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException(error(response));
        }
    }

    private Map<String, Object> toMap(ReunionModel m) {
        Map<String, Object> map = new HashMap<>();
        map.put("titulo", m.getTitulo());
        map.put("fechaHora", m.getFechaHora());
        map.put("lugar", m.getLugar());
        map.put("tipo", m.getTipo());
        return map;
    }

    private record ReunionDto(
        Integer id, String titulo, String fechaHora, String lugar,
        String tipo, String estado, int totalConvocados, int totalAsistentes,
        double porcentajeAsistencia, boolean editable
    ) {
        private ReunionModel toModel() {
            return new ReunionModel(
                id, titulo, fechaHora, lugar, tipo, estado,
                totalConvocados, totalAsistentes, porcentajeAsistencia, editable
            );
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
            Map<?, ?> map = objectMapper.readValue(response.body(), Map.class);
            Object err = map.get("error");
            if (err != null) return err.toString();
        } catch (Exception ignored) {}
        return "Error HTTP " + response.statusCode();
    }
}
