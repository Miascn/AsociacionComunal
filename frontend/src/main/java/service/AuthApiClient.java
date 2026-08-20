package service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public final class AuthApiClient {
    private final QaApiConfig config;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public AuthApiClient() {
        this(QaApiConfig.load());
    }

    AuthApiClient(QaApiConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        this.objectMapper = new ObjectMapper();
    }

    public LoginResponse login(String username, String password) throws IOException, InterruptedException {
        String body = objectMapper.writeValueAsString(new LoginRequest(username, password));
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(config.baseUrl() + "/api/admin/auth/login"))
            .timeout(Duration.ofSeconds(20))
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .header("ngrok-skip-browser-warning", "1")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            String error = response.statusCode() == 401 ? "Credenciales inválidas." : "Error del servidor (HTTP " + response.statusCode() + ").";
            throw new IOException(error);
        }
        return objectMapper.readValue(response.body(), LoginResponse.class);
    }

    public record LoginRequest(String nombreUsuario, String clave) {}
    public record LoginResponse(String token, String displayName, String role, Integer idUsuario) {}
}
