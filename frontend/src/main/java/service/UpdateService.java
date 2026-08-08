package service;

import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.*;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;

public final class UpdateService {
    private UpdateService() {}

    public static void checkAsync(Window owner) {
        if (System.getProperty("jpackage.app-path") == null) return;
        Thread.ofVirtual().start(() -> {
            try {
                QaApiConfig config = QaApiConfig.load();
                HttpClient client = HttpClient.newHttpClient();
                HttpResponse<String> response = client.send(
                    request(config, "/api/updates/windows/manifest-v2").GET().build(),
                    HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200) return;
                Manifest manifest = new ObjectMapper().readValue(response.body(), Manifest.class);
                if (compare(manifest.version(), System.getProperty("jpackage.app-version", "0.0.0")) > 0) {
                    Platform.runLater(() -> offerUpdate(owner, config, client, manifest));
                }
            } catch (Exception ignored) {
                // La comprobacion nunca debe impedir que la aplicacion inicie.
            }
        });
    }

    private static void offerUpdate(Window owner, QaApiConfig config, HttpClient client, Manifest manifest) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
            "La versión " + manifest.version() + " está lista. Solo se descargarán los archivos modificados.",
            ButtonType.OK, ButtonType.CANCEL);
        alert.initOwner(owner);
        alert.setHeaderText("Actualización disponible");
        alert.setTitle("Asociación Comunal QA");
        if (alert.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;
        Stage progress = progressWindow(owner);
        progress.show();
        Thread.ofVirtual().start(() -> install(config, client, manifest, progress));
    }

    private static Stage progressWindow(Window owner) {
        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setPrefSize(48, 48);
        ProgressBar bar = new ProgressBar(ProgressBar.INDETERMINATE_PROGRESS);
        bar.setPrefWidth(320);
        Label status = new Label("Preparando actualización...");
        VBox content = new VBox(14, new HBox(14, spinner, new Label("Actualizando Asociación Comunal")), bar, status);
        content.setPadding(new Insets(24));
        Stage stage = new Stage(StageStyle.UTILITY);
        stage.initOwner(owner);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Actualizando");
        stage.setResizable(false);
        stage.setOnCloseRequest(event -> event.consume());
        stage.setScene(new Scene(content));
        stage.getProperties().put("progress", bar);
        stage.getProperties().put("status", status);
        return stage;
    }

    private static void install(QaApiConfig config, HttpClient client, Manifest manifest, Stage window) {
        Path archive = null;
        try {
            archive = Files.createTempFile("asociacion-delta-", ".zip");
            HttpResponse<InputStream> response = client.send(
                request(config, "/api/updates/windows/delta").GET().build(),
                HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() != 200) throw new IllegalStateException("El servidor no entregó la actualización.");
            long total = response.headers().firstValueAsLong("Content-Length").orElse(manifest.size());
            download(response.body(), archive, total, window);
            if (!sha256(archive).equalsIgnoreCase(manifest.sha256())) {
                throw new IllegalStateException("La descarga no superó la verificación de seguridad.");
            }
            setStatus(window, "Instalando cambios y reiniciando...");
            Path script = Files.createTempFile("asociacion-updater-", ".ps1");
            try (InputStream input = UpdateService.class.getResourceAsStream("/updater/update.ps1")) {
                if (input == null) throw new IllegalStateException("No se encontró el instalador interno.");
                Files.copy(input, script, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            Path executable = Path.of(System.getProperty("jpackage.app-path")).toAbsolutePath();
            new ProcessBuilder("powershell.exe", "-NoProfile", "-ExecutionPolicy", "Bypass", "-File",
                script.toString(), "-InstallDir", executable.getParent().toString(), "-Archive", archive.toString(),
                "-ProcessId", Long.toString(ProcessHandle.current().pid())).start();
            Platform.runLater(Platform::exit);
        } catch (Exception exception) {
            if (archive != null) try { Files.deleteIfExists(archive); } catch (Exception ignored) {}
            Platform.runLater(() -> {
                window.close();
                new Alert(Alert.AlertType.ERROR,
                    "No fue posible instalar la actualización: " + exception.getMessage(), ButtonType.OK).show();
            });
        }
    }

    private static void download(InputStream input, Path target, long total, Stage window) throws Exception {
        try (input; OutputStream output = Files.newOutputStream(target)) {
            byte[] buffer = new byte[64 * 1024];
            long downloaded = 0; int read;
            while ((read = input.read(buffer)) >= 0) {
                output.write(buffer, 0, read);
                downloaded += read;
                long done = downloaded;
                Platform.runLater(() -> {
                    ProgressBar bar = (ProgressBar) window.getProperties().get("progress");
                    Label status = (Label) window.getProperties().get("status");
                    if (total > 0) {
                        bar.setProgress(Math.min(1d, (double) done / total));
                        status.setText("Descargando cambios: " + (done * 100 / total) + "%");
                    } else status.setText("Descargando archivos modificados...");
                });
            }
        }
    }

    private static void setStatus(Stage window, String value) {
        Platform.runLater(() -> ((Label) window.getProperties().get("status")).setText(value));
    }

    private static HttpRequest.Builder request(QaApiConfig config, String path) {
        return HttpRequest.newBuilder(URI.create(config.baseUrl() + path))
            .header("Authorization", "Bearer " + config.token()).header("ngrok-skip-browser-warning", "1");
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

    private record Manifest(String version, String sha256, long size) {}
}
