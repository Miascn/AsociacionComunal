package sv.asociacion.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

public final class AppConfig {
    private static AppConfig instance;

    public final int apiPort;
    public final String apiSharedSecret;
    public final String jwtSecret;

    private AppConfig(int apiPort, String apiSharedSecret, String jwtSecret) {
        this.apiPort = apiPort;
        this.apiSharedSecret = apiSharedSecret;
        this.jwtSecret = jwtSecret;
    }

    public static AppConfig load() {
        if (instance != null) return instance;
        tryLoadDotEnv();
        instance = new AppConfig(
            readPort(),
            requiredSecret("API_SHARED_SECRET"),
            requiredSecret("JWT_SECRET")
        );
        return instance;
    }

    private static void tryLoadDotEnv() {
        Path path = Path.of(".env");
        if (!Files.isRegularFile(path)) {
            path = Path.of("..", ".env");
            if (!Files.isRegularFile(path)) return;
        }
        try (Stream<String> lines = Files.lines(path)) {
            lines.filter(line -> line.contains("=") && !line.stripLeading().startsWith("#"))
                .forEach(line -> {
                    int eq = line.indexOf('=');
                    String key = line.substring(0, eq).trim();
                    String value = line.substring(eq + 1).trim();
                    if (!key.isBlank() && System.getProperty(key) == null) {
                        System.setProperty(key, value);
                    }
                });
        } catch (IOException e) {
            System.err.println("No fue posible leer .env: " + e.getMessage());
        }
    }

    private static int readPort() {
        String value = setting("API_PORT");
        if (value == null || value.isBlank()) return 8080;
        try {
            int port = Integer.parseInt(value.trim());
            if (port < 1024 || port > 65535) {
                throw new IllegalArgumentException("API_PORT debe estar entre 1024 y 65535.");
            }
            return port;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("API_PORT debe ser un numero valido.", e);
        }
    }

    public static String setting(String key) {
        String env = System.getenv(key);
        if (env != null && !env.isBlank()) return env.trim();
        String prop = System.getProperty(key);
        if (prop != null && !prop.isBlank()) return prop.trim();
        return null;
    }

    private static String requiredSecret(String key) {
        String value = setting(key);
        if (value == null || value.length() < 32) {
            throw new IllegalStateException(key + " debe tener al menos 32 caracteres.");
        }
        return value;
    }
}
