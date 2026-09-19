package service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import models.AsistenciaModel;
import security.SessionManager;

public class AsistenciaApiClient {
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final QaApiConfig config;

    public AsistenciaApiClient() {
        this(QaApiConfig.load());
    }

    public AsistenciaApiClient(QaApiConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
        this.objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public List<AsistenciaModel> getByReunion(Integer idReunion) throws IOException, InterruptedException {
        HttpRequest request = requestBuilder("/api/reuniones/" + idReunion + "/asistencias?_t=" + System.currentTimeMillis()).GET().build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException(error(response));
        }

        List<AsistenciaDto> dtos = objectMapper.readValue(response.body(), new TypeReference<>() {});
        return dtos.stream().map(AsistenciaDto::toModel).collect(Collectors.toList());
    }

    public List<AsistenciaModel> convocar(Integer idReunion, List<Integer> miembrosIds) throws IOException, InterruptedException {
        Map<String, Object> map = new HashMap<>();
        if (miembrosIds != null) {
            map.put("miembrosIds", miembrosIds);
        }

        HttpRequest request = requestBuilder("/api/reuniones/" + idReunion + "/asistencias/convocar")
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(map)))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 201) {
            throw new IOException(error(response));
        }

        List<AsistenciaDto> dtos = objectMapper.readValue(response.body(), new TypeReference<>() {});
        return dtos.stream().map(AsistenciaDto::toModel).collect(Collectors.toList());
    }

    public AsistenciaModel registrarOActualizar(Integer idReunion, Integer idMiembro, boolean asistio, String observacion) throws IOException, InterruptedException {
        Map<String, Object> map = new HashMap<>();
        map.put("idMiembro", idMiembro);
        map.put("asistio", asistio);
        map.put("observacion", observacion);

        HttpRequest request = requestBuilder("/api/reuniones/" + idReunion + "/asistencias")
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(map)))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200 && response.statusCode() != 201) {
            throw new IOException(error(response));
        }

        return objectMapper.readValue(response.body(), AsistenciaDto.class).toModel();
    }

    public AsistenciaModel toggle(Long idAsistencia, boolean asistio, String observacion) throws IOException, InterruptedException {
        Map<String, Object> map = new HashMap<>();
        map.put("asistio", asistio);
        map.put("observacion", observacion);

        HttpRequest request = requestBuilder("/api/asistencias/" + idAsistencia)
            .header("Content-Type", "application/json")
            .method("PATCH", HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(map)))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException(error(response));
        }

        return objectMapper.readValue(response.body(), AsistenciaDto.class).toModel();
    }

    public List<AsistenciaModel> guardarLote(Integer idReunion, List<AsistenciaModel> items) throws IOException, InterruptedException {
        List<Map<String, Object>> payload = items.stream().map(m -> {
            Map<String, Object> map = new HashMap<>();
            map.put("idMiembro", m.getIdMiembro());
            map.put("asistio", m.isAsistio());
            map.put("observacion", m.getObservacion());
            return map;
        }).collect(Collectors.toList());

        HttpRequest request = requestBuilder("/api/reuniones/" + idReunion + "/asistencias/lote")
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException(error(response));
        }

        List<AsistenciaDto> dtos = objectMapper.readValue(response.body(), new TypeReference<>() {});
        return dtos.stream().map(AsistenciaDto::toModel).collect(Collectors.toList());
    }

    public void delete(Long idAsistencia) throws IOException, InterruptedException {
        HttpRequest request = requestBuilder("/api/asistencias/" + idAsistencia).DELETE().build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException(error(response));
        }
    }

    private record AsistenciaDto(
        Long idAsistencia, Integer idReunion, Integer idMiembro,
        String nombreMiembro, String duiMiembro, String telefonoMiembro,
        boolean asistio, String observacion
    ) {
        private AsistenciaModel toModel() {
            return new AsistenciaModel(
                idAsistencia, idReunion, idMiembro,
                nombreMiembro, duiMiembro, telefonoMiembro,
                asistio, observacion
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

    private String error(HttpResponse<String> response) {
        try {
            Map<?, ?> map = objectMapper.readValue(response.body(), Map.class);
            Object err = map.get("error");
            if (err != null) return err.toString();
        } catch (Exception ignored) {}
        return "Error HTTP " + response.statusCode();
    }
}
