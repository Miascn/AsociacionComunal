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
    private final QaApiConfig config = QaApiConfig.load();
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final ObjectMapper json = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public List<RolModel> findAll() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(config.baseUrl() + "/api/roles"))
            .timeout(Duration.ofSeconds(20)).header("Authorization", "Bearer " + SessionManager.getInstance().requireToken())
            .header("Accept", "application/json").header("ngrok-skip-browser-warning", "1").GET().build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) throw new IOException(error(response));
        return Arrays.asList(json.readValue(response.body(), RolModel[].class));
    }

    private String error(HttpResponse<String> response) {
        try { return json.readTree(response.body()).path("error").asText("HTTP " + response.statusCode()); }
        catch (Exception ignored) { return "HTTP " + response.statusCode(); }
    }
}
