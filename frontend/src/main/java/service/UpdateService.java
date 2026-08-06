package service;

import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;

public final class UpdateService {
    private UpdateService() {}

    public static void checkAsync() {
        if (System.getProperty("jpackage.app-path") == null) return;
        Thread.ofVirtual().start(() -> {
            try {
                QaApiConfig config = QaApiConfig.load();
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest manifestRequest = request(config, "/api/updates/windows/manifest").GET().build();
                HttpResponse<String> manifestResponse = client.send(manifestRequest, HttpResponse.BodyHandlers.ofString());
                if (manifestResponse.statusCode() != 200) return;
                Manifest manifest = new ObjectMapper().readValue(manifestResponse.body(), Manifest.class);
                String current = System.getProperty("jpackage.app-version", "0.0.0");
                if (compare(manifest.version(), current) <= 0) return;
                Platform.runLater(() -> offerUpdate(config, client, manifest));
            } catch (Exception ignored) {
                // Una falla de actualización nunca debe impedir usar la aplicación.
            }
        });
    }

    private static void offerUpdate(QaApiConfig config, HttpClient client, Manifest manifest) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
            "Está disponible la versión " + manifest.version() + ". Se descargará y reiniciará la aplicación.",
            ButtonType.OK, ButtonType.CANCEL);
        alert.setHeaderText("Actualización disponible");
        if (alert.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;
        Thread.ofVirtual().start(() -> install(config, client, manifest));
    }

    private static void install(QaApiConfig config, HttpClient client, Manifest manifest) {
        try {
            Path archive = Files.createTempFile("asociacion-update-", ".zip");
            HttpResponse<Path> response = client.send(
                request(config, "/api/updates/windows/package").GET().build(),
                HttpResponse.BodyHandlers.ofFile(archive));
            if (response.statusCode() != 200 || !sha256(archive).equalsIgnoreCase(manifest.sha256())) {
                Files.deleteIfExists(archive);
                throw new IllegalStateException("La descarga no superó la verificación de seguridad.");
            }
            Path script = Files.createTempFile("asociacion-updater-", ".ps1");
            try (InputStream input = UpdateService.class.getResourceAsStream("/updater/update.ps1")) {
                Files.copy(input, script, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            Path executable = Path.of(System.getProperty("jpackage.app-path")).toAbsolutePath();
            new ProcessBuilder("powershell.exe", "-NoProfile", "-ExecutionPolicy", "Bypass",
                "-File", script.toString(), "-InstallDir", executable.getParent().toString(),
                "-Archive", archive.toString(), "-ProcessId", Long.toString(ProcessHandle.current().pid()))
                .start();
            Platform.exit();
        } catch (Exception exception) {
            Platform.runLater(() -> new Alert(Alert.AlertType.ERROR,
                "No fue posible instalar la actualización: " + exception.getMessage(), ButtonType.OK).show());
        }
    }

    private static HttpRequest.Builder request(QaApiConfig config, String path) {
        return HttpRequest.newBuilder(URI.create(config.baseUrl() + path))
            .header("Authorization", "Bearer " + config.token())
            .header("ngrok-skip-browser-warning", "1");
    }

    private static String sha256(Path path) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream input = Files.newInputStream(path)) {
            byte[] buffer = new byte[8192]; int read;
            while ((read = input.read(buffer)) >= 0) digest.update(buffer, 0, read);
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    static int compare(String a, String b) {
        String[] left = a.split("\\."); String[] right = b.split("\\.");
        for (int i = 0; i < Math.max(left.length, right.length); i++) {
            int l = i < left.length ? Integer.parseInt(left[i]) : 0;
            int r = i < right.length ? Integer.parseInt(right[i]) : 0;
            if (l != r) return Integer.compare(l, r);
        }
        return 0;
    }

    private record Manifest(String version, String sha256) {}
}
