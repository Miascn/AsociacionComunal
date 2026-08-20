package sv.asociacion.service;

import io.javalin.http.UploadedFile;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;

public class AndroidUpdateService {
    private final Path updatesDir;

    public AndroidUpdateService() {
        this(Path.of(System.getProperty("user.home"), "apps", "asociacion-api", "android-updates"));
    }
    AndroidUpdateService(Path updatesDir) { this.updatesDir = updatesDir; }

    public Map<String, Object> manifest() throws Exception {
        Path version = updatesDir.resolve("version.txt");
        Path archive = updatesDir.resolve("AsociacionComunalAndroid.apk");
        Path checksum = updatesDir.resolve("sha256.txt");
        if (!Files.isRegularFile(version) || !Files.isRegularFile(archive) || !Files.isRegularFile(checksum)) return null;
        Path notes = updatesDir.resolve("notes.txt");
        return Map.of(
            "version", Files.readString(version).trim(), "sha256", Files.readString(checksum).trim(),
            "size", Files.size(archive),
            "notes", Files.isRegularFile(notes) ? Files.readString(notes).trim() : "Mejoras y correcciones.",
            "downloadPath", "/api/mobile/updates/android/package"
        );
    }

    public InputStream packageStream() throws Exception {
        Path archive = updatesDir.resolve("AsociacionComunalAndroid.apk");
        return Files.isRegularFile(archive) ? Files.newInputStream(archive) : null;
    }

    public Map<String, Object> publish(UploadedFile apk, UploadedFile versionFile, UploadedFile checksumFile, UploadedFile notesFile) throws Exception {
        String version = new String(versionFile.content().readAllBytes(), StandardCharsets.UTF_8).trim();
        String checksum = new String(checksumFile.content().readAllBytes(), StandardCharsets.UTF_8).trim().toLowerCase();
        if (!version.matches("\\d+\\.\\d+\\.\\d+") || !checksum.matches("[a-f0-9]{64}")) {
            throw new IllegalArgumentException("Versión o checksum inválido.");
        }
        Files.createDirectories(updatesDir);
        Path staging = Files.createTempDirectory(updatesDir, ".publish-");
        try {
            Path stagedApk = staging.resolve("AsociacionComunalAndroid.apk");
            Files.copy(apk.content(), stagedApk);
            if (!sha256(stagedApk).equals(checksum)) throw new IllegalArgumentException("El checksum del APK no coincide.");
            Files.writeString(staging.resolve("version.txt"), version, StandardCharsets.UTF_8);
            Files.writeString(staging.resolve("sha256.txt"), checksum, StandardCharsets.UTF_8);
            String notes = notesFile == null ? "Mejoras y correcciones." : new String(notesFile.content().readAllBytes(), StandardCharsets.UTF_8);
            Files.writeString(staging.resolve("notes.txt"), notes, StandardCharsets.UTF_8);
            for (String name : new String[]{"AsociacionComunalAndroid.apk", "version.txt", "sha256.txt", "notes.txt"}) {
                Files.move(staging.resolve(name), updatesDir.resolve(name), StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            try (var files = Files.list(staging)) {
                files.forEach(path -> { try { Files.deleteIfExists(path); } catch (Exception ignored) { } });
            }
            Files.deleteIfExists(staging);
        }
        return Map.of("version", version, "published", true);
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
}
