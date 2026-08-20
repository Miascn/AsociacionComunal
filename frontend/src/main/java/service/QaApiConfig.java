package service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public record QaApiConfig(String baseUrl, String token) {
    public static QaApiConfig load() {
        String environmentUrl = System.getenv("QA_API_URL");
        String environmentToken = System.getenv("QA_API_TOKEN");
        if (hasText(environmentUrl) && hasText(environmentToken)) {
            return validated(environmentUrl, environmentToken);
        }
        for (Path candidate : configurationCandidates()) {
            if (Files.isRegularFile(candidate)) {
                return fromFile(candidate);
            }
        }
        throw new IllegalStateException(
            "No se encontro config/qa.properties. Reinstale el paquete QA autorizado."
        );
    }

    private static QaApiConfig fromFile(Path path) {
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(path)) {
            properties.load(input);
        } catch (IOException exception) {
            throw new IllegalStateException("No fue posible leer la configuracion QA.", exception);
        }
        return validated(properties.getProperty("api.url"), properties.getProperty("api.token"));
    }

    private static QaApiConfig validated(String url, String token) {
        if (!hasText(url) || !url.trim().startsWith("https://")) {
            throw new IllegalStateException("La URL de la API QA debe usar HTTPS.");
        }
        if (!hasText(token) || token.trim().length() < 32) {
            throw new IllegalStateException("El token de la API QA no es valido.");
        }
        return new QaApiConfig(url.trim().replaceAll("/+$", ""), token.trim());
    }

    private static List<Path> configurationCandidates() {
        List<Path> candidates = new ArrayList<>();
        String explicit = System.getProperty("qa.config");
        if (hasText(explicit)) {
            candidates.add(Path.of(explicit.trim()));
        }
        String executable = System.getProperty("jpackage.app-path");
        if (hasText(executable)) {
            Path parent = Path.of(executable).toAbsolutePath().getParent();
            if (parent != null) {
                candidates.add(parent.resolve("config").resolve("qa.properties"));
            }
        }
        Path directory = Path.of("").toAbsolutePath();
        for (int level = 0; directory != null && level < 4; level++) {
            candidates.add(directory.resolve("qa-local.properties"));
            directory = directory.getParent();
        }
        return candidates;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
