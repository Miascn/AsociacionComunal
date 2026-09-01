package service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import models.RolModel;
import security.SessionManager;

public final class RolApiClient {
    private final QaApiConfig config;
    private final HttpClient client;
    private final ObjectMapper json;

    public RolApiClient() {
        this(QaApiConfig.load());
    }

    RolApiClient(QaApiConfig config) {
        this.config = config;
        this.client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        this.json = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public List<RolModel> findAll() throws IOException, InterruptedException {
        HttpRequest request = requestBuilder("/api/roles").GET().build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) throw new IOException(error(response));
        return Arrays.asList(json.readValue(response.body(), RolModel[].class));
    }

    public RolModel findById(Integer id) throws IOException, InterruptedException {
        HttpRequest request = requestBuilder("/api/roles/" + id).GET().build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) throw new IOException(error(response));
        return json.readValue(response.body(), RolModel.class);
    }

    public RolModel create(RolRequest request) throws IOException, InterruptedException {
        String body = json.writeValueAsString(request);
        HttpRequest httpRequest = requestBuilder("/api/roles")
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();
        HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 201) throw new IOException(error(response));
        return json.readValue(response.body(), RolModel.class);
    }

    public RolModel update(Integer id, RolRequest request) throws IOException, InterruptedException {
        String body = json.writeValueAsString(request);
        HttpRequest httpRequest = requestBuilder("/api/roles/" + id)
            .header("Content-Type", "application/json")
            .PUT(HttpRequest.BodyPublishers.ofString(body))
            .build();
        HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) throw new IOException(error(response));
        return json.readValue(response.body(), RolModel.class);
    }

    public boolean delete(Integer id) throws IOException, InterruptedException {
        HttpRequest httpRequest = requestBuilder("/api/roles/" + id).DELETE().build();
        HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 204) throw new IOException(error(response));
        return true;
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
            return json.readTree(response.body()).path("error").asText("HTTP " + response.statusCode());
        } catch (Exception ignored) {
            return "HTTP " + response.statusCode();
        }
    }

    public record RolRequest(String nombre, String descripcion) {}
}
