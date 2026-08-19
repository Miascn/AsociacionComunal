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
import java.util.function.Consumer;

public final class UpdateService {
    private UpdateService() {}

    public static void checkAsync(Window owner) {
        checkAsync(owner, status -> {}, false);
    }

    public static void checkManually(Window owner, Consumer<String> statusConsumer) {
        checkAsync(owner, statusConsumer, true);
    }

    private static void checkAsync(Window owner, Consumer<String> statusConsumer, boolean manual) {
        String currentVersion = System.getProperty("jpackage.app-version");
        if (currentVersion == null || currentVersion.isBlank()) {
            publishStatus(statusConsumer, "Modo DEV: el actualizador solo funciona en el paquete QA");
            if (manual) showInfo(owner, "Actualizaciones", "Estás ejecutando la aplicación desde desarrollo.\n\nLa búsqueda automática solo funciona en el ejecutable QA generado con jpackage.");
            return;
        }

        publishStatus(statusConsumer, "Buscando actualizaciones...");
        Thread.ofVirtual().start(() -> {
            try {
                QaApiConfig config = QaApiConfig.load();
                HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(java.time.Duration.ofSeconds(10))
                    .build();
                HttpResponse<String> response = client.send(
                    request(config, "/api/updates/windows/manifest-v2")
                        .timeout(java.time.Duration.ofSeconds(20))
                        .GET().build(),
                    HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 404) {
                    String message = "El servidor aún no tiene una actualización QA publicada";
                    publishStatus(statusConsumer, message);
                    if (manual) showInfo(owner, "Sin actualización publicada", message + ".");
                    return;
                }
                if (response.statusCode() != 200) {
                    throw new IllegalStateException("El servidor respondió HTTP " + response.statusCode());
                }

                Manifest manifest = new ObjectMapper().readValue(response.body(), Manifest.class);
                if (manifest.version() == null || manifest.version().isBlank()) {
                    throw new IllegalStateException("El manifiesto no contiene una versión válida");
                }

                int comparison = compare(manifest.version(), currentVersion);
                if (comparison > 0) {
                    publishStatus(statusConsumer, "Nueva versión disponible: " + manifest.version());
                    Platform.runLater(() -> offerUpdate(owner, config, client, manifest));
                } else {
                    String message = "Actualizado: versión " + currentVersion;
                    publishStatus(statusConsumer, message);
                    if (manual) showInfo(owner, "Aplicación actualizada", "Ya tienes la versión más reciente (" + currentVersion + ").");
                }
            } catch (Exception exception) {
                String detail = readableError(exception);
                publishStatus(statusConsumer, "Error al buscar actualizaciones: " + detail);
                if (manual) showError(owner, "No se pudo buscar actualizaciones", detail);
                else System.err.println("[QA Update] " + detail);
            }
        });
    }

    private static String readableError(Exception exception) {
        Throwable cause = exception;
        while (cause.getCause() != null) cause = cause.getCause();
        String message = exception.getMessage();
        if (message == null || message.isBlank()) message = cause.getMessage();
        if (message == null || message.isBlank()) message = exception.getClass().getSimpleName();
        return message;
    }

    private static void publishStatus(Consumer<String> consumer, String status) {
        Platform.runLater(() -> consumer.accept(status));
    }

    private static void showInfo(Window owner, String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
            if (owner != null) alert.initOwner(owner);
            alert.setTitle("Asociación Comunal QA");
            alert.setHeaderText(title);
            alert.showAndWait();
        });
    }

    private static void showError(Window owner, String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
            if (owner != null) alert.initOwner(owner);
            alert.setTitle("Asociación Comunal QA");
            alert.setHeaderText(title);
            alert.showAndWait();
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
                request(config, "/api/updates/windows/delta")
                    .timeout(java.time.Duration.ofMinutes(3))
                    .GET().build(),
                HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() != 200) throw new IllegalStateException("El servidor no entregó la actualización (HTTP " + response.statusCode() + ").");
            long total = response.headers().firstValueAsLong("Content-Length").orElse(manifest.size());
            download(response.body(), archive, total, window);
            if (!sha256(archive).equalsIgnoreCase(manifest.sha256())) {
                throw new IllegalStateException("La descarga no superó la verificación SHA-256.");
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
            String detail = readableError(exception);
            Platform.runLater(() -> {
                window.close();
                new Alert(Alert.AlertType.ERROR,
                    "No fue posible instalar la actualización: " + detail, ButtonType.OK).show();
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

    private record Manifest(String version, String sha256, long size) {}
}
