package sv.asociacion.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.http.UploadedFile;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;

public class UpdateService {
    private static final ObjectMapper JSON = new ObjectMapper();
    private final Path updatesDir;

    public UpdateService() {
        this.updatesDir = Path.of(System.getProperty("user.home"), "apps", "asociacion-api", "updates");
    }

    public UpdateService(Path updatesDir) {
        this.updatesDir = updatesDir;
    }

    public Path getUpdatesDir() {
        return updatesDir;
    }

    public Map<String, Object> getManifestV1() throws Exception {
        Path version = updatesDir.resolve("version.txt");
        Path archive = updatesDir.resolve("AsociacionComunalQA-win64.zip");
        Path checksum = updatesDir.resolve("sha256.txt");
        if (!Files.isRegularFile(version) || !Files.isRegularFile(archive) || !Files.isRegularFile(checksum)) {
            return null;
        }
        return Map.of("version", Files.readString(version).trim(), "sha256", Files.readString(checksum).trim());
    }

    public Map<String, Object> getManifestV2() throws Exception {
        Path manifest = updatesDir.resolve("update-manifest.json");
        Path archive = updatesDir.resolve("AsociacionComunalQA-delta.zip");
        if (!Files.isRegularFile(manifest) || !Files.isRegularFile(archive)) {
            return null;
        }
        String manifestJson = Files.readString(manifest);
        if (!manifestJson.isEmpty() && manifestJson.charAt(0) == '\uFEFF') {
            manifestJson = manifestJson.substring(1);
        }
        JsonNode published = JSON.readTree(manifestJson);
        return Map.of(
            "version", published.path("version").asText(),
            "sha256", published.path("sha256").asText(),
            "size", published.path("size").asLong()
        );
    }

    public InputStream getBuildManifest() throws Exception {
        Path manifest = updatesDir.resolve("update-manifest.json");
        if (!Files.isRegularFile(manifest)) {
            return null;
        }
        return Files.newInputStream(manifest);
    }

    public InputStream getLegacyPackage() throws Exception {
        Path archive = updatesDir.resolve("AsociacionComunalQA-win64.zip");
        if (!Files.isRegularFile(archive)) {
            return null;
        }
        return Files.newInputStream(archive);
    }

    public InputStream getDeltaPackage() throws Exception {
        Path archive = updatesDir.resolve("AsociacionComunalQA-delta.zip");
        if (!Files.isRegularFile(archive)) {
            return null;
        }
        return Files.newInputStream(archive);
    }

    public Map<String, Object> publish(UploadedFile full, UploadedFile delta, UploadedFile manifestUpload,
                                        UploadedFile versionUpload, UploadedFile checksumUpload) throws Exception {
        byte[] manifestBytes = manifestUpload.content().readAllBytes();
        byte[] versionBytes = versionUpload.content().readAllBytes();
        byte[] checksumBytes = checksumUpload.content().readAllBytes();
        JsonNode manifest = JSON.readTree(manifestBytes);
        String version = new String(versionBytes, StandardCharsets.UTF_8).trim();
        String fullChecksum = new String(checksumBytes, StandardCharsets.UTF_8).trim().toLowerCase();
        if (!version.matches("\\d+\\.\\d+\\.\\d+") || !version.equals(manifest.path("version").asText())) {
            throw new IllegalArgumentException("La versión y el manifiesto no coinciden.");
        }
        String deltaChecksum = manifest.path("sha256").asText().toLowerCase();
        if (!fullChecksum.matches("[a-f0-9]{64}") || !deltaChecksum.matches("[a-f0-9]{64}")) {
            throw new IllegalArgumentException("Los checksums publicados no son válidos.");
        }
        Path currentVersion = updatesDir.resolve("version.txt");
        if (Files.isRegularFile(currentVersion)
            && compareVersions(version, Files.readString(currentVersion).trim()) < 0) {
            throw new IllegalArgumentException("No se puede publicar una versión anterior a la instalada.");
        }

        Files.createDirectories(updatesDir);
        Path staging = Files.createTempDirectory(updatesDir, ".publish-");
        try {
            Path fullFile = staging.resolve("AsociacionComunalQA-" + version + "-win64.zip");
            Path deltaFile = staging.resolve("AsociacionComunalQA-" + version + "-delta.zip");
            Files.copy(full.content(), fullFile);
            Files.copy(delta.content(), deltaFile);
            if (!sha256(fullFile).equals(fullChecksum)) {
                throw new IllegalArgumentException("El checksum del paquete completo no coincide.");
            }
            if (!sha256(deltaFile).equals(deltaChecksum)
                || Files.size(deltaFile) != manifest.path("size").asLong()) {
                throw new IllegalArgumentException("El paquete incremental no coincide con su manifiesto.");
            }
            Files.write(staging.resolve("update-manifest.json"), manifestBytes);
            Files.write(staging.resolve("version.txt"), versionBytes);
            Files.write(staging.resolve("sha256.txt"), checksumBytes);

            publishFile(fullFile, updatesDir.resolve(fullFile.getFileName()));
            publishFile(deltaFile, updatesDir.resolve(deltaFile.getFileName()));
            Files.copy(updatesDir.resolve(fullFile.getFileName()), updatesDir.resolve("AsociacionComunalQA-win64.zip"), StandardCopyOption.REPLACE_EXISTING);
            Files.copy(updatesDir.resolve(deltaFile.getFileName()), updatesDir.resolve("AsociacionComunalQA-delta.zip"), StandardCopyOption.REPLACE_EXISTING);
            publishFile(staging.resolve("update-manifest.json"), updatesDir.resolve("update-manifest.json"));
            publishFile(staging.resolve("version.txt"), updatesDir.resolve("version.txt"));
            publishFile(staging.resolve("sha256.txt"), updatesDir.resolve("sha256.txt"));
        } finally {
            try (var files = Files.list(staging)) {
                files.forEach(path -> { try { Files.deleteIfExists(path); } catch (Exception ignored) { } });
            }
            Files.deleteIfExists(staging);
        }
        return Map.of("version", version, "published", true);
    }

    private static void publishFile(Path source, Path destination) throws Exception {
        Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING);
    }

    private static String sha256(Path file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream input = Files.newInputStream(file)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) >= 0) digest.update(buffer, 0, read);
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private static int compareVersions(String left, String right) {
        String[] a = left.split("\\.");
        String[] b = right.split("\\.");
        for (int index = 0; index < 3; index++) {
            int comparison = Integer.compare(Integer.parseInt(a[index]), Integer.parseInt(b[index]));
            if (comparison != 0) return comparison;
        }
        return 0;
    }
}
