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
import java.time.Duration;
import java.util.HexFormat;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class UpdateService {
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration MANIFEST_TIMEOUT = Duration.ofSeconds(20);
    private static final Duration DOWNLOAD_TIMEOUT = Duration.ofMinutes(3);
    private static final AtomicBoolean STARTED = new AtomicBoolean(false);
    private static final AtomicBoolean CHECKING = new AtomicBoolean(false);
    private static volatile String lastOfferedVersion;

    private static final ScheduledExecutorService SCHEDULER =
        Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "qa-update-scheduler");
            thread.setDaemon(true);
            return thread;
        });

    private UpdateService() {}

    /**
     * Inicia el actualizador automático una sola vez. Comprueba poco después del
     * arranque y vuelve a comprobar periódicamente mientras la aplicación siga abierta.
     */
    public static void startAutomatic(Window owner) {
        if (!STARTED.compareAndSet(false, true)) return;
        SCHEDULER.schedule(() -> checkAsync(owner), 3, TimeUnit.SECONDS);
        SCHEDULER.scheduleWithFixedDelay(() -> checkAsync(owner), 15, 15, TimeUnit.MINUTES);
    }

    /** Versión visible de la aplicación. */
    public static String currentVersion() {
        String explicit = System.getProperty("asociacion.app.version");
        if (hasText(explicit)) return explicit.trim();

        // Compatibilidad con paquetes QA antiguos que pudieran exponer esta propiedad.
        String legacy = System.getProperty("jpackage.app-version");
        if (hasText(legacy)) return legacy.trim();

        // Si estamos empaquetados pero la versión antigua no la expone, forzamos
        // una actualización hacia la versión publicada más reciente.
        return applicationExecutable() != null ? "0.0.0" : "DEV";
    }

    private static void checkAsync(Window owner) {
        if (!CHECKING.compareAndSet(false, true)) return;

        Thread.ofVirtual().start(() -> {
            try {
                if (applicationExecutable() == null) return;

                String currentVersion = currentVersion();
                QaApiConfig config = QaApiConfig.load();
                HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(CONNECT_TIMEOUT)
                    .build();

                HttpResponse<String> response = client.send(
                    request(config, "/api/updates/windows/manifest-v2")
                        .timeout(MANIFEST_TIMEOUT)
                        .GET().build(),
                    HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 404) {
                    System.err.println("[QA Update] No hay una actualización QA publicada.");
                    return;
                }
                if (response.statusCode() != 200) {
                    throw new IllegalStateException("El servidor respondió HTTP " + response.statusCode());
                }

                Manifest manifest = new ObjectMapper().readValue(response.body(), Manifest.class);
                if (!hasText(manifest.version())) {
                    throw new IllegalStateException("El manifiesto no contiene una versión válida.");
                }

                if (compare(manifest.version(), currentVersion) > 0
                    && !manifest.version().equals(lastOfferedVersion)) {
                    lastOfferedVersion = manifest.version();
                    Platform.runLater(() -> offerUpdate(owner, config, client, manifest));
                }
            } catch (Exception exception) {
                System.err.println("[QA Update] No se pudo comprobar la actualización: " + readableError(exception));
            } finally {
                CHECKING.set(false);
            }
        });
    }

    private static void offerUpdate(Window owner, QaApiConfig config, HttpClient client, Manifest manifest) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
            "La versión " + manifest.version() + " está lista. Solo se descargarán los archivos modificados.",
            ButtonType.OK, ButtonType.CANCEL);
        if (owner != null) alert.initOwner(owner);
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
        VBox content = new VBox(14,
            new HBox(14, spinner, new Label("Actualizando Asociación Comunal")),
            bar,
            status);
        content.setPadding(new Insets(24));

        Stage stage = new Stage(StageStyle.UTILITY);
        if (owner != null) stage.initOwner(owner);
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
            Path executable = applicationExecutable();
            if (executable == null || executable.getParent() == null) {
                throw new IllegalStateException("No se pudo localizar la instalación QA.");
            }

            archive = Files.createTempFile("asociacion-delta-", ".zip");
            HttpResponse<InputStream> response = client.send(
                request(config, "/api/updates/windows/delta")
                    .timeout(DOWNLOAD_TIMEOUT)
                    .GET().build(),
                HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() != 200) {
                throw new IllegalStateException("El servidor no entregó la actualización (HTTP " + response.statusCode() + ").");
            }

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

            new ProcessBuilder(
                "powershell.exe", "-NoProfile", "-ExecutionPolicy", "Bypass", "-File",
                script.toString(),
                "-InstallDir", executable.getParent().toString(),
                "-Archive", archive.toString(),
                "-ProcessId", Long.toString(ProcessHandle.current().pid()),
                "-LauncherProcessId", Long.toString(ProcessHandle.current().parent().map(ProcessHandle::pid).orElse(0L))
            ).start();

            Platform.runLater(Platform::exit);
        } catch (Exception exception) {
            if (archive != null) {
                try { Files.deleteIfExists(archive); } catch (Exception ignored) {}
            }
            String detail = readableError(exception);
            Platform.runLater(() -> {
                window.close();
                Alert error = new Alert(Alert.AlertType.ERROR,
                    "No fue posible instalar la actualización: " + detail,
                    ButtonType.OK);
                if (window.getOwner() != null) error.initOwner(window.getOwner());
                error.setTitle("Asociación Comunal QA");
                error.setHeaderText("Error al actualizar");
                error.showAndWait();
            });
        }
    }

    private static Path applicationExecutable() {
        String jpackagePath = System.getProperty("jpackage.app-path");
        if (hasText(jpackagePath)) {
            try {
                Path path = Path.of(jpackagePath).toAbsolutePath().normalize();
                if (Files.isRegularFile(path)) return path;
            } catch (Exception ignored) {}
        }

        try {
            String command = ProcessHandle.current().info().command().orElse(null);
            if (hasText(command)) {
                Path path = Path.of(command).toAbsolutePath().normalize();
                String name = path.getFileName() == null ? "" : path.getFileName().toString();
                if (Files.isRegularFile(path) && name.equalsIgnoreCase("AsociacionComunalQA.exe")) {
                    return path;
                }
            }
        } catch (Exception ignored) {}

        return null;
    }

    private static void download(InputStream input, Path target, long total, Stage window) throws Exception {
        try (input; OutputStream output = Files.newOutputStream(target)) {
            byte[] buffer = new byte[64 * 1024];
            long downloaded = 0;
            int read;
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
                    } else {
                        status.setText("Descargando archivos modificados...");
                    }
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
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) >= 0) digest.update(buffer, 0, read);
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private static String readableError(Exception exception) {
        Throwable cause = exception;
        while (cause.getCause() != null) cause = cause.getCause();
        String message = exception.getMessage();
        if (!hasText(message)) message = cause.getMessage();
        if (!hasText(message)) message = exception.getClass().getSimpleName();
        return message;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    static int compare(String a, String b) {
        if (a == null || b == null) return 0;
        String[] left = a.trim().split("\\.");
        String[] right = b.trim().split("\\.");
        for (int i = 0; i < Math.max(left.length, right.length); i++) {
            int l = i < left.length ? parseSegment(left[i]) : 0;
            int r = i < right.length ? parseSegment(right[i]) : 0;
            if (l != r) return Integer.compare(l, r);
        }
        return 0;
    }

    private static int parseSegment(String segment) {
        if (segment == null || segment.isBlank()) return 0;
        String clean = segment.replaceAll("[^0-9].*$", "");
        if (clean.isEmpty()) return 0;
        try {
            return Integer.parseInt(clean);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private record Manifest(String version, String sha256, long size) {}
}
