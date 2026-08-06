package sv.asociacion.api;

import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import sv.asociacion.backend.config.DBConnection;
import sv.asociacion.backend.dao.MiembroDAO;
import sv.asociacion.backend.entity.Miembro;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public final class ApiServer {
    private static final String HOST = "127.0.0.1";
    private static final int DEFAULT_PORT = 8080;

    private ApiServer() {
    }

    public static void main(String[] args) {
        int port = readPort();
        String apiSecret = requiredSecret();
        MiembroDAO miembroDAO = new MiembroDAO();

        Javalin.start(config -> {
            config.jetty.host = HOST;
            config.jetty.port = port;
            config.http.maxRequestSize = 1_048_576L;
            config.routes.before("/api/*", context -> authenticate(context, apiSecret));
            config.routes.get("/health", ApiServer::health);
            config.routes.get("/api/miembros", context -> context.json(toResponse(miembroDAO.findAll())));
            config.routes.post("/api/miembros", context -> createMember(context, miembroDAO));
            config.routes.exception(Exception.class, (exception, context) -> {
                exception.printStackTrace(System.err);
                context.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .json(Map.of("error", "No fue posible procesar la solicitud."));
            });
        });
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
}
