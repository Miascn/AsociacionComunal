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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import models.AsignacionCargoModel;
import security.SessionManager;

public final class DirectivaApiClient {
    private final QaApiConfig config;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public DirectivaApiClient() {
        this(QaApiConfig.load());
    }

    DirectivaApiClient(QaApiConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        this.objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public List<AsignacionCargoModel> findDirectivaActual() throws IOException, InterruptedException {
        HttpRequest request = requestBuilder("/api/directiva/actual").GET().build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException(error(response));
        }
        AsignacionResponse[] list = objectMapper.readValue(response.body(), AsignacionResponse[].class);
        return Arrays.stream(list).map(AsignacionResponse::toModel).toList();
    }

    public List<AsignacionCargoModel> findDirectivaPeriodo(Integer idPeriodo) throws IOException, InterruptedException {
        HttpRequest request = requestBuilder("/api/directiva/periodo/" + idPeriodo).GET().build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException(error(response));
        }
        AsignacionResponse[] list = objectMapper.readValue(response.body(), AsignacionResponse[].class);
        return Arrays.stream(list).map(AsignacionResponse::toModel).toList();
    }

    public List<AsignacionCargoModel> findFiltered(Integer idPeriodo, String estado, String busqueda) throws IOException, InterruptedException {
        StringBuilder query = new StringBuilder("/api/asignaciones-cargo?");
        if (idPeriodo != null) {
            query.append("&periodoId=").append(idPeriodo);
        }
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
        AsignacionResponse[] list = objectMapper.readValue(response.body(), AsignacionResponse[].class);
        return Arrays.stream(list).map(AsignacionResponse::toModel).toList();
    }

    public AsignacionCargoModel create(Integer idMiembro, Integer idCargo, Integer idPeriodo,
                                       String fechaAsignacion, String fechaFin) throws IOException, InterruptedException {
        Map<String, Object> map = new HashMap<>();
        map.put("idMiembro", idMiembro);
        map.put("idCargo", idCargo);
        map.put("idPeriodo", idPeriodo);
        map.put("fechaAsignacion", fechaAsignacion);
        if (fechaFin != null && !fechaFin.isBlank()) {
            map.put("fechaFin", fechaFin);
        }

        String body = objectMapper.writeValueAsString(map);
        HttpRequest request = requestBuilder("/api/asignaciones-cargo")
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 201) {
            throw new IOException(error(response));
        }
        return objectMapper.readValue(response.body(), AsignacionResponse.class).toModel();
    }

    public AsignacionCargoModel revocar(Integer id, String fechaFin, String motivo) throws IOException, InterruptedException {
        Map<String, Object> map = new HashMap<>();
        if (fechaFin != null && !fechaFin.isBlank()) {
            map.put("fechaFin", fechaFin);
        }
        if (motivo != null && !motivo.isBlank()) {
            map.put("motivo", motivo);
        }

        String body = objectMapper.writeValueAsString(map);
        HttpRequest request = requestBuilder("/api/asignaciones-cargo/" + id + "/revocar")
            .header("Content-Type", "application/json")
            .method("PATCH", HttpRequest.BodyPublishers.ofString(body))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException(error(response));
        }
        return objectMapper.readValue(response.body(), AsignacionResponse.class).toModel();
    }

    public AsignacionCargoModel finalizar(Integer id) throws IOException, InterruptedException {
        HttpRequest request = requestBuilder("/api/asignaciones-cargo/" + id + "/finalizar")
            .method("PATCH", HttpRequest.BodyPublishers.noBody())
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException(error(response));
        }
        return objectMapper.readValue(response.body(), AsignacionResponse.class).toModel();
    }

    public void delete(Integer id) throws IOException, InterruptedException {
        HttpRequest request = requestBuilder("/api/asignaciones-cargo/" + id).DELETE().build();
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

    private record AsignacionResponse(
        Integer id, Integer idMiembro, String nombreMiembro, String duiMiembro,
        String telefonoMiembro, Integer idCargo, String nombreCargo,
        Integer nivelJerarquico, Integer idPeriodo, String nombrePeriodo,
        String estadoPeriodo, String fechaAsignacion, String fechaFin,
        String motivoSalida, String estado
    ) {
        private AsignacionCargoModel toModel() {
            return new AsignacionCargoModel(
                id, idMiembro, nombreMiembro, duiMiembro, telefonoMiembro,
                idCargo, nombreCargo, nivelJerarquico, idPeriodo, nombrePeriodo,
                estadoPeriodo, fechaAsignacion, fechaFin, motivoSalida, estado
            );
        }
    }
}
