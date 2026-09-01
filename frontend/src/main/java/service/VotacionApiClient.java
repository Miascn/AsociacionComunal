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
import models.VotacionModel;
import security.SessionManager;

public final class VotacionApiClient {
    private final QaApiConfig config;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public VotacionApiClient() {
        this(QaApiConfig.load());
    }

    VotacionApiClient(QaApiConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        this.objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public List<VotacionModel> findAll() throws IOException, InterruptedException {
        return findFiltered(null, null, null);
    }

    public List<VotacionModel> findFiltered(String estado, Integer idProyecto, String busqueda) throws IOException, InterruptedException {
        StringBuilder query = new StringBuilder("/api/votaciones?");
        if (estado != null && !estado.isBlank() && !"TODOS".equalsIgnoreCase(estado)) {
            query.append("&estado=").append(encode(estado.trim()));
        }
        if (idProyecto != null) {
            query.append("&proyectoId=").append(idProyecto);
        }
        if (busqueda != null && !busqueda.isBlank()) {
            query.append("&busqueda=").append(encode(busqueda.trim()));
        }

        HttpRequest request = requestBuilder(query.toString()).GET().build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException(error(response));
        }

        VotacionResponseDto[] list = objectMapper.readValue(response.body(), VotacionResponseDto[].class);
        return Arrays.stream(list).map(VotacionResponseDto::toModel).toList();
    }

    public VotacionModel findById(Integer id) throws IOException, InterruptedException {
        HttpRequest request = requestBuilder("/api/votaciones/" + id).GET().build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException(error(response));
        }
        return objectMapper.readValue(response.body(), VotacionResponseDto.class).toModel();
    }

    public VotacionModel create(String titulo, String descripcion, Integer idProyecto,
                                String fechaInicio, String fechaFin, List<String> opcionesIniciales) throws IOException, InterruptedException {
        Map<String, Object> map = new HashMap<>();
        map.put("titulo", titulo);
        map.put("descripcion", descripcion);
        map.put("idProyecto", idProyecto);
        map.put("fechaInicio", fechaInicio);
        map.put("fechaFin", fechaFin);
        if (opcionesIniciales != null && !opcionesIniciales.isEmpty()) {
            map.put("opcionesIniciales", opcionesIniciales);
        }

        String body = objectMapper.writeValueAsString(map);
        HttpRequest request = requestBuilder("/api/votaciones")
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 201) {
            throw new IOException(error(response));
        }
        return objectMapper.readValue(response.body(), VotacionResponseDto.class).toModel();
    }

    public VotacionModel update(Integer id, String titulo, String descripcion, Integer idProyecto,
                                String fechaInicio, String fechaFin) throws IOException, InterruptedException {
        Map<String, Object> map = new HashMap<>();
        map.put("titulo", titulo);
        map.put("descripcion", descripcion);
        map.put("idProyecto", idProyecto);
        map.put("fechaInicio", fechaInicio);
        map.put("fechaFin", fechaFin);

        String body = objectMapper.writeValueAsString(map);
        HttpRequest request = requestBuilder("/api/votaciones/" + id)
            .header("Content-Type", "application/json")
            .PUT(HttpRequest.BodyPublishers.ofString(body))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException(error(response));
        }
        return objectMapper.readValue(response.body(), VotacionResponseDto.class).toModel();
    }

    public VotacionModel abrir(Integer id) throws IOException, InterruptedException {
        HttpRequest request = requestBuilder("/api/votaciones/" + id + "/abrir")
            .method("PATCH", HttpRequest.BodyPublishers.noBody())
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException(error(response));
        }
        return objectMapper.readValue(response.body(), VotacionResponseDto.class).toModel();
    }

    public VotacionModel cerrar(Integer id) throws IOException, InterruptedException {
        HttpRequest request = requestBuilder("/api/votaciones/" + id + "/cerrar")
            .method("PATCH", HttpRequest.BodyPublishers.noBody())
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException(error(response));
        }
        return objectMapper.readValue(response.body(), VotacionResponseDto.class).toModel();
    }

    public VotacionModel cancelar(Integer id) throws IOException, InterruptedException {
        HttpRequest request = requestBuilder("/api/votaciones/" + id + "/cancelar")
            .method("PATCH", HttpRequest.BodyPublishers.noBody())
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException(error(response));
        }
        return objectMapper.readValue(response.body(), VotacionResponseDto.class).toModel();
    }

    public void delete(Integer id) throws IOException, InterruptedException {
        HttpRequest request = requestBuilder("/api/votaciones/" + id).DELETE().build();
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

    private record VotacionResponseDto(
        Integer id, String titulo, String descripcion, Integer idProyecto,
        String nombreProyecto, String fechaInicio, String fechaFin,
        String estado, int totalOpciones, int totalVotos, List<OpcionDto> opciones
    ) {
        private VotacionModel toModel() {
            List<VotacionModel.OpcionModel> ops = opciones != null
                ? opciones.stream().map(o -> new VotacionModel.OpcionModel(o.idOpcion, o.descripcion, o.orden, o.votos)).toList()
                : List.of();
            return new VotacionModel(
                id, titulo, descripcion, idProyecto, nombreProyecto, fechaInicio, fechaFin, estado, totalOpciones, totalVotos, ops
            );
        }
    }

    private record OpcionDto(Integer idOpcion, String descripcion, int orden, int votos) {}
}
