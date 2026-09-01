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
import models.BitacoraModel;
import security.SessionManager;

public final class BitacoraApiClient {
    private final QaApiConfig config;
    private final HttpClient client;
    private final ObjectMapper json;

    public BitacoraApiClient() {
        this(QaApiConfig.load());
    }

    BitacoraApiClient(QaApiConfig config) {
        this.config = config;
        this.client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        this.json = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public BitacoraModel.Page findPage(String usuario, String entidad, String accion,
                                      String desde, String hasta, int page, int size)
        throws IOException, InterruptedException {

        StringBuilder query = new StringBuilder("/api/bitacoras?");
        query.append("page=").append(page);
        query.append("&size=").append(size);

        if (usuario != null && !usuario.isBlank()) {
            query.append("&usuario=").append(encode(usuario.trim()));
        }
        if (entidad != null && !entidad.isBlank() && !"TODAS".equalsIgnoreCase(entidad.trim())) {
            query.append("&entidad=").append(encode(entidad.trim()));
        }
        if (accion != null && !accion.isBlank() && !"TODAS".equalsIgnoreCase(accion.trim())) {
            query.append("&accion=").append(encode(accion.trim()));
        }
        if (desde != null && !desde.isBlank()) {
            query.append("&desde=").append(encode(desde.trim()));
        }
        if (hasta != null && !hasta.isBlank()) {
            query.append("&hasta=").append(encode(hasta.trim()));
        }

        HttpRequest request = requestBuilder(query.toString()).GET().build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException(error(response));
        }

        return json.readValue(response.body(), BitacoraModel.Page.class);
    }

    public BitacoraModel findById(Long id) throws IOException, InterruptedException {
        HttpRequest request = requestBuilder("/api/bitacoras/" + id).GET().build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException(error(response));
        }

        return json.readValue(response.body(), BitacoraModel.Page.class).items().get(0);
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
