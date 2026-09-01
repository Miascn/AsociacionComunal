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
import java.util.Map;
import models.AportacionModel;
import security.SessionManager;

public final class AportacionApiClient {
    private final QaApiConfig config;
    private final HttpClient client;
    private final ObjectMapper json;

    public AportacionApiClient() {
        this(QaApiConfig.load());
    }

    AportacionApiClient(QaApiConfig config) {
        this.config = config;
        this.client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        this.json = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public AportacionModel.Page findPage(Integer idMiembro, Integer idProyecto, String periodo,
                                         String desde, String hasta, String metodo, String estado,
                                         String busqueda, int page, int size)
        throws IOException, InterruptedException {

        StringBuilder query = new StringBuilder("/api/aportaciones?");
        query.append("page=").append(page);
        query.append("&size=").append(size);

        if (idMiembro != null) query.append("&idMiembro=").append(idMiembro);
        if (idProyecto != null) query.append("&idProyecto=").append(idProyecto);
        if (periodo != null && !periodo.isBlank()) query.append("&periodo=").append(encode(periodo.trim()));
        if (desde != null && !desde.isBlank()) query.append("&desde=").append(encode(desde.trim()));
        if (hasta != null && !hasta.isBlank()) query.append("&hasta=").append(encode(hasta.trim()));
        if (metodo != null && !metodo.isBlank() && !"TODOS".equalsIgnoreCase(metodo)) {
            query.append("&metodo=").append(encode(metodo.trim()));
        }
        if (estado != null && !estado.isBlank() && !"TODOS".equalsIgnoreCase(estado)) {
            query.append("&estado=").append(encode(estado.trim()));
        }
        if (busqueda != null && !busqueda.isBlank()) query.append("&busqueda=").append(encode(busqueda.trim()));

        HttpRequest request = requestBuilder(query.toString()).GET().build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException(error(response));
        }

        return json.readValue(response.body(), AportacionModel.Page.class);
    }

    public AportacionModel findById(Long id) throws IOException, InterruptedException {
        HttpRequest request = requestBuilder("/api/aportaciones/" + id).GET().build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException(error(response));
        }

        return json.readValue(response.body(), AportacionModel.class);
    }

    public AportacionModel create(AportacionModel model) throws IOException, InterruptedException {
        String body = json.writeValueAsString(Map.of(
            "idMiembro", model.getIdMiembro(),
            "idProyecto", model.getIdProyecto() != null ? model.getIdProyecto() : "",
            "periodoMes", model.getPeriodoMes(),
            "monto", model.getMonto(),
            "fechaPago", model.getFechaPago(),
            "metodoPago", model.getMetodoPago(),
            "referencia", model.getReferencia() != null ? model.getReferencia() : ""
        ));

        HttpRequest request = requestBuilder("/api/aportaciones")
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 201) {
            throw new IOException(error(response));
        }
        return json.readValue(response.body(), AportacionModel.class);
    }

    public AportacionModel update(Long id, AportacionModel model) throws IOException, InterruptedException {
        String body = json.writeValueAsString(Map.of(
            "idMiembro", model.getIdMiembro(),
            "idProyecto", model.getIdProyecto() != null ? model.getIdProyecto() : "",
            "periodoMes", model.getPeriodoMes(),
            "monto", model.getMonto(),
            "fechaPago", model.getFechaPago(),
            "metodoPago", model.getMetodoPago(),
            "referencia", model.getReferencia() != null ? model.getReferencia() : ""
        ));

        HttpRequest request = requestBuilder("/api/aportaciones/" + id)
            .header("Content-Type", "application/json")
            .PUT(HttpRequest.BodyPublishers.ofString(body))
            .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException(error(response));
        }
        return json.readValue(response.body(), AportacionModel.class);
    }

    public void anular(Long id) throws IOException, InterruptedException {
        HttpRequest request = requestBuilder("/api/aportaciones/" + id + "/anular")
            .method("PATCH", HttpRequest.BodyPublishers.noBody())
            .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
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
            return json.readTree(response.body()).path("error").asText("HTTP " + response.statusCode());
        } catch (Exception ignored) {
            return "HTTP " + response.statusCode();
        }
    }
}
