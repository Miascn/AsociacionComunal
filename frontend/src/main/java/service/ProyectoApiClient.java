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
import models.ProyectoModel;
import security.SessionManager;

public final class ProyectoApiClient {
    private final QaApiConfig config;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public ProyectoApiClient() {
        this(QaApiConfig.load());
    }

    ProyectoApiClient(QaApiConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        this.objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public List<ProyectoModel> findAll() throws IOException, InterruptedException {
        return findFiltered(null, null);
    }

    public List<ProyectoModel> findFiltered(String estado, String busqueda) throws IOException, InterruptedException {
        StringBuilder query = new StringBuilder("/api/proyectos?");
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

        ProyectoResponse[] values = objectMapper.readValue(response.body(), ProyectoResponse[].class);
        return Arrays.stream(values).map(ProyectoResponse::toModel).toList();
    }

    public ProyectoModel findById(Integer id) throws IOException, InterruptedException {
        HttpRequest request = requestBuilder("/api/proyectos/" + id).GET().build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException(error(response));
        }

        return objectMapper.readValue(response.body(), ProyectoResponse.class).toModel();
    }

    public ProyectoModel create(ProyectoModel model) throws IOException, InterruptedException {
        String body = objectMapper.writeValueAsString(Map.of(
            "nombre", model.getNombre(),
            "descripcion", model.getDescripcion() != null ? model.getDescripcion() : "",
            "presupuesto", model.getPresupuesto(),
            "creadoPor", model.getCreadoPor() != null ? model.getCreadoPor() : "",
            "estado", model.getEstado() != null ? model.getEstado() : "BORRADOR"
        ));

        HttpRequest request = requestBuilder("/api/proyectos")
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 201) {
            throw new IOException(error(response));
        }
        return objectMapper.readValue(response.body(), ProyectoResponse.class).toModel();
    }

    public ProyectoModel update(Integer id, ProyectoModel model) throws IOException, InterruptedException {
        String body = objectMapper.writeValueAsString(Map.of(
            "nombre", model.getNombre(),
            "descripcion", model.getDescripcion() != null ? model.getDescripcion() : "",
            "presupuesto", model.getPresupuesto()
        ));

        HttpRequest request = requestBuilder("/api/proyectos/" + id)
            .header("Content-Type", "application/json")
            .PUT(HttpRequest.BodyPublishers.ofString(body))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException(error(response));
        }
        return objectMapper.readValue(response.body(), ProyectoResponse.class).toModel();
    }

    public ProyectoModel cambiarEstado(Integer id, String nuevoEstado) throws IOException, InterruptedException {
        String body = objectMapper.writeValueAsString(Map.of("nuevoEstado", nuevoEstado));

        HttpRequest request = requestBuilder("/api/proyectos/" + id + "/estado")
            .header("Content-Type", "application/json")
            .method("PATCH", HttpRequest.BodyPublishers.ofString(body))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException(error(response));
        }
        return objectMapper.readValue(response.body(), ProyectoResponse.class).toModel();
    }

    public void delete(Integer id) throws IOException, InterruptedException {
        HttpRequest request = requestBuilder("/api/proyectos/" + id).DELETE().build();
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

    private record ProyectoResponse(
        Integer id, Integer creadoPor, String nombreCreador, String nombre, String descripcion,
        java.math.BigDecimal presupuesto, String fechaCreacion, String estado
    ) {
        private ProyectoModel toModel() {
            return new ProyectoModel(id, creadoPor, nombreCreador, nombre, descripcion, presupuesto, fechaCreacion, estado);
        }
    }
}