package sv.asociacion.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import io.javalin.http.UploadedFile;
import sv.asociacion.backend.config.DBConnection;
import sv.asociacion.backend.dao.MiembroDAO;
import sv.asociacion.backend.dao.ProyectoDAO;
import sv.asociacion.backend.entity.Miembro;
import sv.asociacion.backend.entity.Proyecto;
import sv.asociacion.api.auth.AuthRoutes;
import sv.asociacion.api.auth.AuthService;
import sv.asociacion.api.auth.JdbcSessionRepository;
import sv.asociacion.api.auth.JdbcUserAuthRepository;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public final class ApiServer {
    private static final String HOST = "127.0.0.1";
    private static final int DEFAULT_PORT = 8080;
    private static final ObjectMapper JSON = new ObjectMapper();

    private ApiServer() {
    }

    public static void main(String[] args) {
        int port = readPort();
        String apiSecret = requiredSecret();
        MiembroDAO miembroDAO = new MiembroDAO();
        ProyectoDAO proyectoDAO = new ProyectoDAO();

        Javalin app = Javalin.create(config -> {
            config.jetty.host = HOST;
            config.jetty.port = port;
            config.http.maxRequestSize = 268_435_456L;
            config.routes.before("/api/miembros", context -> authenticate(context, apiSecret));
            config.routes.before("/api/proyectos", context -> authenticate(context, apiSecret));
            config.routes.before("/api/updates/*", context -> authenticate(context, apiSecret));
            config.routes.get("/health", ApiServer::health);
            config.routes.get("/api/miembros", context -> context.json(toResponse(miembroDAO.findAll())));
            config.routes.post("/api/miembros", context -> createMember(context, miembroDAO));
            config.routes.get("/api/proyectos", context -> context.json(toProjectResponse(proyectoDAO.findAll())));
            config.routes.get("/api/updates/windows/manifest", ApiServer::updateManifest);
            config.routes.get("/api/updates/windows/manifest-v2", ApiServer::updateManifestV2);
            config.routes.get("/api/updates/windows/build-manifest", ApiServer::buildManifest);
            config.routes.get("/api/updates/windows/package", ApiServer::legacyUpdatePackage);
            config.routes.get("/api/updates/windows/delta", ApiServer::updatePackage);
            config.routes.post("/api/updates/windows/publish", ApiServer::publishUpdate);
            new AuthRoutes(new AuthService(new JdbcUserAuthRepository(), new JdbcSessionRepository())).register(config.routes);
            config.routes.exception(Exception.class, (exception, context) -> {
                exception.printStackTrace(System.err);
                context.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .json(Map.of("error", "No fue posible procesar la solicitud."));
            });
        });
        app.start(HOST, port);
        System.out.printf("Asociacion API disponible en http://%s:%d%n", HOST, port);
    }

    private static void authenticate(Context context, String expectedSecret) {
        String authorization = context.header("Authorization");
        String suppliedSecret = authorization != null && authorization.startsWith("Bearer ")
            ? authorization.substring("Bearer ".length()).trim()
            : "";

        boolean matches = MessageDigest.isEqual(
            expectedSecret.getBytes(StandardCharsets.UTF_8),
            suppliedSecret.getBytes(StandardCharsets.UTF_8)
        );
        if (!matches) {
            context.header("WWW-Authenticate", "Bearer")
                .status(HttpStatus.UNAUTHORIZED)
                .json(Map.of("error", "No autorizado."));
            context.skipRemainingHandlers();
        }
    }

    private static void health(Context context) {
        try (Connection connection = DBConnection.getInstance().getConnection()) {
            boolean valid = connection.isValid(3);
            context.status(valid ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE)
                .json(Map.of("service", "asociacion-api", "database", valid ? "available" : "unavailable"));
        } catch (SQLException | IllegalStateException exception) {
            context.status(HttpStatus.SERVICE_UNAVAILABLE)
                .json(Map.of("service", "asociacion-api", "database", "unavailable"));
        }
    }

    private static List<MiembroResponse> toResponse(List<Miembro> miembros) {
        return miembros.stream().map(MiembroResponse::from).toList();
    }

    private static List<ProyectoResponse> toProjectResponse(List<Proyecto> proyectos) {
        return proyectos.stream().map(ProyectoResponse::from).toList();
    }

    private static void createMember(Context context, MiembroDAO miembroDAO) {
        CreateMemberRequest request = context.bodyAsClass(CreateMemberRequest.class);
        String validationError = request.validationError();
        if (validationError != null) {
            context.status(HttpStatus.BAD_REQUEST).json(Map.of("error", validationError));
            return;
        }
        if (miembroDAO.existsByDui(request.dui().trim())) {
            context.status(HttpStatus.CONFLICT)
                .json(Map.of("error", "Ya existe un miembro con ese DUI."));
            return;
        }
        Miembro miembro = new Miembro(
            null,
            request.dui().trim(),
            request.nombres().trim(),
            request.apellidos().trim(),
            clean(request.telefono()),
            clean(request.correo()),
            request.direccion().trim(),
            LocalDate.now(),
            Miembro.Estado.ACTIVO
        );
        miembroDAO.save(miembro);
        context.status(HttpStatus.CREATED).json(MiembroResponse.from(miembro));
    }

    private static String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static Path updateDirectory() {
        return Path.of(System.getProperty("user.home"), "apps", "asociacion-api", "updates");
    }

    private static void updateManifest(Context context) throws Exception {
        Path version = updateDirectory().resolve("version.txt");
        Path archive = updateDirectory().resolve("AsociacionComunalQA-win64.zip");
        Path checksum = updateDirectory().resolve("sha256.txt");
        if (!Files.isRegularFile(version) || !Files.isRegularFile(archive) || !Files.isRegularFile(checksum)) {
            context.status(HttpStatus.NOT_FOUND).json(Map.of("error", "No hay actualización publicada."));
            return;
        }
        context.json(Map.of("version", Files.readString(version).trim(), "sha256", Files.readString(checksum).trim()));
    }

    private static void updateManifestV2(Context context) throws Exception {
        Path manifest = updateDirectory().resolve("update-manifest.json");
        Path archive = updateDirectory().resolve("AsociacionComunalQA-delta.zip");
        if (!Files.isRegularFile(manifest) || !Files.isRegularFile(archive)) {
            context.status(HttpStatus.NOT_FOUND).json(Map.of("error", "No hay actualización publicada."));
            return;
        }
        String manifestJson = Files.readString(manifest);
        if (!manifestJson.isEmpty() && manifestJson.charAt(0) == '\uFEFF') {
            manifestJson = manifestJson.substring(1);
        }
        JsonNode published = JSON.readTree(manifestJson);
        context.json(Map.of(
            "version", published.path("version").asText(),
            "sha256", published.path("sha256").asText(),
            "size", published.path("size").asLong()
        ));
    }

    private static void buildManifest(Context context) throws Exception {
        Path manifest = updateDirectory().resolve("update-manifest.json");
        if (!Files.isRegularFile(manifest)) {
            context.status(HttpStatus.NOT_FOUND).json(Map.of("error", "No hay manifiesto publicado."));
            return;
        }
        context.contentType("application/json").result(Files.newInputStream(manifest));
    }

    private static void legacyUpdatePackage(Context context) throws Exception {
        Path archive = updateDirectory().resolve("AsociacionComunalQA-win64.zip");
        if (!Files.isRegularFile(archive)) {
            context.status(HttpStatus.NOT_FOUND); return;
        }
        context.header("Content-Disposition", "attachment; filename=AsociacionComunalQA-win64.zip");
        context.contentType("application/zip").result(Files.newInputStream(archive));
    }

    private static void updatePackage(Context context) throws Exception {
        Path archive = updateDirectory().resolve("AsociacionComunalQA-delta.zip");
        if (!Files.isRegularFile(archive)) {
            context.status(HttpStatus.NOT_FOUND); return;
        }
        context.header("Content-Disposition", "attachment; filename=AsociacionComunalQA-delta.zip");
        context.contentType("application/zip").result(Files.newInputStream(archive));
    }

    private static void publishUpdate(Context context) throws Exception {
        UploadedFile full = requiredUpload(context, "full");
        UploadedFile delta = requiredUpload(context, "delta");
        UploadedFile manifestUpload = requiredUpload(context, "manifest");
        UploadedFile versionUpload = requiredUpload(context, "version");
        UploadedFile checksumUpload = requiredUpload(context, "checksum");

        byte[] manifestBytes = manifestUpload.content().readAllBytes();
        byte[] versionBytes = versionUpload.content().readAllBytes();
        byte[] checksumBytes = checksumUpload.content().readAllBytes();
        JsonNode manifest = JSON.readTree(manifestBytes);
        String version = new String(versionBytes, StandardCharsets.UTF_8).trim();
        if (!version.matches("\\d+\\.\\d+\\.\\d+") || !version.equals(manifest.path("version").asText())) {
            context.status(HttpStatus.BAD_REQUEST).json(Map.of("error", "La versión y el manifiesto no coinciden."));
            return;
        }

        Path updates = updateDirectory();
        Files.createDirectories(updates);
        Path staging = Files.createTempDirectory(updates, ".publish-");
        try {
            Path fullFile = staging.resolve("AsociacionComunalQA-" + version + "-win64.zip");
            Path deltaFile = staging.resolve("AsociacionComunalQA-" + version + "-delta.zip");
            Files.copy(full.content(), fullFile);
            Files.copy(delta.content(), deltaFile);
            Files.write(staging.resolve("update-manifest.json"), manifestBytes);
            Files.write(staging.resolve("version.txt"), versionBytes);
            Files.write(staging.resolve("sha256.txt"), checksumBytes);

            publishFile(fullFile, updates.resolve(fullFile.getFileName()));
            publishFile(deltaFile, updates.resolve(deltaFile.getFileName()));
            Files.copy(updates.resolve(fullFile.getFileName()), updates.resolve("AsociacionComunalQA-win64.zip"), StandardCopyOption.REPLACE_EXISTING);
            Files.copy(updates.resolve(deltaFile.getFileName()), updates.resolve("AsociacionComunalQA-delta.zip"), StandardCopyOption.REPLACE_EXISTING);
            publishFile(staging.resolve("update-manifest.json"), updates.resolve("update-manifest.json"));
            publishFile(staging.resolve("version.txt"), updates.resolve("version.txt"));
            publishFile(staging.resolve("sha256.txt"), updates.resolve("sha256.txt"));
        } finally {
            try (var files = Files.list(staging)) {
                files.forEach(path -> { try { Files.deleteIfExists(path); } catch (Exception ignored) { } });
            }
            Files.deleteIfExists(staging);
        }
        context.status(HttpStatus.CREATED).json(Map.of("version", version, "published", true));
    }

    private static UploadedFile requiredUpload(Context context, String name) {
        UploadedFile file = context.uploadedFile(name);
        if (file == null) throw new IllegalArgumentException("Falta el archivo " + name + ".");
        return file;
    }

    private static void publishFile(Path source, Path destination) throws Exception {
        Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING);
    }

    private record CreateMemberRequest(
        String dui,
        String nombres,
        String apellidos,
        String telefono,
        String correo,
        String direccion
    ) {
        private String validationError() {
            if (dui == null || !dui.trim().matches("\\d{8}-\\d")) {
                return "El DUI debe tener el formato 00000000-0.";
            }
            if (nombres == null || nombres.isBlank() || apellidos == null || apellidos.isBlank()) {
                return "Los nombres y apellidos son obligatorios.";
            }
            if (direccion == null || direccion.isBlank()) {
                return "La direccion es obligatoria.";
            }
            if (telefono != null && !telefono.isBlank() && !telefono.trim().matches("\\d{4}-\\d{4}")) {
                return "El telefono debe tener el formato 0000-0000.";
            }
            if (correo != null && !correo.isBlank()
                && !correo.trim().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
                return "El correo electronico no es valido.";
            }
            return null;
        }
    }

    private static int readPort() {
        String configured = System.getenv("API_PORT");
        if (configured == null || configured.isBlank()) {
            return DEFAULT_PORT;
        }
        try {
            int port = Integer.parseInt(configured);
            if (port < 1024 || port > 65535) {
                throw new IllegalArgumentException("API_PORT debe estar entre 1024 y 65535.");
            }
            return port;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("API_PORT debe ser un numero valido.", exception);
        }
    }

    private static String requiredSecret() {
        String secret = System.getenv("API_SHARED_SECRET");
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException("API_SHARED_SECRET debe tener al menos 32 caracteres.");
        }
        return secret;
    }

    private record MiembroResponse(
        Integer id,
        String dui,
        String nombres,
        String apellidos,
        String telefono,
        String correo,
        String direccion,
        String fechaIngreso,
        String estado
    ) {
        private static MiembroResponse from(Miembro miembro) {
            return new MiembroResponse(
                miembro.getIdMiembro(),
                miembro.getDui(),
                miembro.getNombres(),
                miembro.getApellidos(),
                miembro.getTelefono(),
                miembro.getCorreo(),
                miembro.getDireccion(),
                miembro.getFechaIngreso() == null ? null : miembro.getFechaIngreso().toString(),
                miembro.getEstado() == null ? null : miembro.getEstado().name()
            );
        }
    }

    private record ProyectoResponse(
        Integer id,
        String nombre,
        String descripcion,
        java.math.BigDecimal presupuesto,
        String fechaCreacion,
        String estado
    ) {
        private static ProyectoResponse from(Proyecto proyecto) {
            return new ProyectoResponse(
                proyecto.getIdProyecto(),
                proyecto.getNombre(),
                proyecto.getDescripcion(),
                proyecto.getPresupuesto(),
                proyecto.getFechaCreacion() == null ? null : proyecto.getFechaCreacion().toString(),
                proyecto.getEstado() == null ? null : proyecto.getEstado().name()
            );
        }
    }
}
