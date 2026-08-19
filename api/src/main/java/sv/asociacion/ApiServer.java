package sv.asociacion;

import io.javalin.Javalin;
import io.javalin.http.HttpStatus;
import sv.asociacion.config.AppConfig;
import sv.asociacion.controller.AuthController;
import sv.asociacion.controller.HealthController;
import sv.asociacion.controller.MiembroController;
import sv.asociacion.controller.ProyectoController;
import sv.asociacion.controller.UpdateController;
import sv.asociacion.controller.UsuarioController;
import sv.asociacion.middleware.AuthMiddleware;
import sv.asociacion.middleware.JwtAuthMiddleware;
import sv.asociacion.service.AuthService;
import sv.asociacion.service.MiembroService;
import sv.asociacion.service.ProyectoService;
import sv.asociacion.service.UpdateService;
import sv.asociacion.service.UsuarioService;
import sv.asociacion.dao.MiembroDAO;
import sv.asociacion.dao.ProyectoDAO;
import sv.asociacion.dao.UsuarioDAO;
import sv.asociacion.dao.RolDAO;
import java.util.Map;

public final class ApiServer {
    private static final String HOST = "127.0.0.1";

    private ApiServer() {
    }

    public static void main(String[] args) {
        AppConfig config = AppConfig.load();

        MiembroDAO miembroDAO = new MiembroDAO();
        ProyectoDAO proyectoDAO = new ProyectoDAO();
        UsuarioDAO usuarioDAO = new UsuarioDAO();
        RolDAO rolDAO = new RolDAO();

        AuthService authService = new AuthService(usuarioDAO, rolDAO, miembroDAO, config.jwtSecret);
        MiembroService miembroService = new MiembroService(miembroDAO);
        ProyectoService proyectoService = new ProyectoService(proyectoDAO);
        UpdateService updateService = new UpdateService();
        UsuarioService usuarioService = new UsuarioService(usuarioDAO);

        AuthController authController = new AuthController(authService);
        HealthController healthController = new HealthController();
        MiembroController miembroController = new MiembroController(miembroService);
        ProyectoController proyectoController = new ProyectoController(proyectoService);
        UpdateController updateController = new UpdateController(updateService);
        UsuarioController usuarioController = new UsuarioController(usuarioService);

        AuthMiddleware sharedSecretAuth = new AuthMiddleware(config.apiSharedSecret);
        JwtAuthMiddleware jwtAuth = new JwtAuthMiddleware(authService);

        Javalin.start(cfg -> {
            cfg.jetty.host = HOST;
            cfg.jetty.port = config.apiPort;
            cfg.http.maxRequestSize = 268_435_456L;

            cfg.routes.before("/api/updates/*", sharedSecretAuth::authenticate);
            cfg.routes.before("/api/*", jwtAuth::authenticate);
            cfg.routes.get("/health", healthController::health);
            cfg.routes.post("/api/auth/login", authController::login);
            cfg.routes.get("/api/miembros", miembroController::getAll);
            cfg.routes.post("/api/miembros", miembroController::create);
            cfg.routes.get("/api/proyectos", proyectoController::getAll);
            cfg.routes.get("/api/usuarios", usuarioController::getAll);
            cfg.routes.get("/api/usuarios/{id}", usuarioController::getById);
            cfg.routes.post("/api/usuarios", usuarioController::create);
            cfg.routes.put("/api/usuarios/{id}", usuarioController::update);
            cfg.routes.delete("/api/usuarios/{id}", usuarioController::delete);
            cfg.routes.get("/api/updates/windows/manifest", updateController::manifestV1);
            cfg.routes.get("/api/updates/windows/manifest-v2", updateController::manifestV2);
            cfg.routes.get("/api/updates/windows/build-manifest", updateController::buildManifest);
            cfg.routes.get("/api/updates/windows/package", updateController::legacyPackage);
            cfg.routes.get("/api/updates/windows/delta", updateController::deltaPackage);
            cfg.routes.post("/api/updates/windows/publish", updateController::publish);
            cfg.routes.exception(Exception.class, (exception, context) -> {
                exception.printStackTrace(System.err);
                context.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .json(Map.of("error", "No fue posible procesar la solicitud."));
            });
        });
        System.out.printf("Asociacion API disponible en http://%s:%d%n", HOST, config.apiPort);
    }
}