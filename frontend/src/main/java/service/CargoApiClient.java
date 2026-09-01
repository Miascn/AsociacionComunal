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
import models.CargoModel;
import security.SessionManager;

public final class CargoApiClient {
    private final QaApiConfig config;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public CargoApiClient() {
        this(QaApiConfig.load());
    }

    CargoApiClient(QaApiConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        this.objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public List<CargoModel> findAll() throws IOException, InterruptedException {
        return findFiltered(null, null);
    }

    public List<CargoModel> findFiltered(Boolean activo, String busqueda) throws IOException, InterruptedException {
        StringBuilder query = new StringBuilder("/api/cargos?");
        if (activo != null) {
            query.append("&activo=").append(activo);
        }
        if (busqueda != null && !busqueda.isBlank()) {
            query.append("&busqueda=").append(encode(busqueda.trim()));
        }

        HttpRequest request = requestBuilder(query.toString()).GET().build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException(error(response));
        }

        CargoResponse[] values = objectMapper.readValue(response.body(), CargoResponse[].class);
        return Arrays.stream(values).map(CargoResponse::toModel).toList();
    }

    public CargoModel findById(Integer id) throws IOException, InterruptedException {
        HttpRequest request = requestBuilder("/api/cargos/" + id).GET().build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException(error(response));
        }

        return objectMapper.readValue(response.body(), CargoResponse.class).toModel();
    }

    public CargoModel create(CargoModel model) throws IOException, InterruptedException {
        String body = objectMapper.writeValueAsString(Map.of(
            "nombre", model.getNombre(),
            "descripcion", model.getDescripcion() != null ? model.getDescripcion() : "",
            "nivelJerarquico", model.getNivelJerarquico() != null ? model.getNivelJerarquico() : 1,
            "activo", model.getActivo() != null ? model.getActivo() : true
        ));

        HttpRequest request = requestBuilder("/api/cargos")
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 201) {
            throw new IOException(error(response));
        }
        return objectMapper.readValue(response.body(), CargoResponse.class).toModel();
    }

    public CargoModel update(Integer id, CargoModel model) throws IOException, InterruptedException {
        String body = objectMapper.writeValueAsString(Map.of(
            "nombre", model.getNombre(),
            "descripcion", model.getDescripcion() != null ? model.getDescripcion() : "",
            "nivelJerarquico", model.getNivelJerarquico() != null ? model.getNivelJerarquico() : 1,
            "activo", model.getActivo() != null ? model.getActivo() : true
        ));

        HttpRequest request = requestBuilder("/api/cargos/" + id)
            .header("Content-Type", "application/json")
            .PUT(HttpRequest.BodyPublishers.ofString(body))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException(error(response));
        }
        return objectMapper.readValue(response.body(), CargoResponse.class).toModel();
    }

    public CargoModel toggleActivo(Integer id, boolean activo) throws IOException, InterruptedException {
        HttpRequest request = requestBuilder("/api/cargos/" + id + "/desactivar?activo=" + activo)
            .method("PATCH", HttpRequest.BodyPublishers.noBody())
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException(error(response));
        }
        return objectMapper.readValue(response.body(), CargoResponse.class).toModel();
    }

    public void delete(Integer id) throws IOException, InterruptedException {
        HttpRequest request = requestBuilder("/api/cargos/" + id).DELETE().build();
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

    private record CargoResponse(
        Integer id, String nombre, String descripcion, Integer nivelJerarquico, Boolean activo, Integer totalAsignaciones
    ) {
        private CargoModel toModel() {
            return new CargoModel(id, nombre, descripcion, nivelJerarquico, activo, totalAsignaciones);
        }
    }
}
