package sv.asociacion;

import io.javalin.Javalin;
import io.javalin.http.HttpStatus;
import java.util.Map;
import sv.asociacion.api.auth.AuthRoutes;
import sv.asociacion.api.auth.JdbcSessionRepository;
import sv.asociacion.api.auth.JdbcUserAuthRepository;
import sv.asociacion.api.auth.MemberProvisioningService;
import sv.asociacion.config.AppConfig;
import sv.asociacion.controller.*;
import sv.asociacion.dao.*;
import sv.asociacion.middleware.AuthMiddleware;
import sv.asociacion.middleware.JwtAuthMiddleware;
import sv.asociacion.service.*;

public final class ApiServer {
    private static final String HOST = "127.0.0.1";

    private ApiServer() { }

    public static void main(String[] args) {
        AppConfig config = AppConfig.load();

        MiembroDAO miembroDAO = new MiembroDAO();
        ProyectoDAO proyectoDAO = new ProyectoDAO();
        UsuarioDAO usuarioDAO = new UsuarioDAO();
        RolDAO rolDAO = new RolDAO();
        ViviendaDAO viviendaDAO = new ViviendaDAO();

        sv.asociacion.service.AuthService jwtService =
            new sv.asociacion.service.AuthService(usuarioDAO, rolDAO, miembroDAO, config.jwtSecret);
        MiembroService miembroService = new MiembroService(miembroDAO, new MemberProvisioningService());
        ProyectoService proyectoService = new ProyectoService(proyectoDAO);
        UsuarioService usuarioService = new UsuarioService(usuarioDAO);
        ViviendaService viviendaService = new ViviendaService(viviendaDAO);

        AuthController adminAuth = new AuthController(jwtService);
        HealthController health = new HealthController();
        MiembroController miembros = new MiembroController(miembroService);
        ProyectoController proyectos = new ProyectoController(proyectoService);
        UsuarioController usuarios = new UsuarioController(usuarioService);
        ViviendaController viviendas = new ViviendaController(viviendaService);
        UpdateController updates = new UpdateController(new UpdateService());
        AndroidUpdateController androidUpdates = new AndroidUpdateController(new AndroidUpdateService());

        AuthMiddleware sharedSecretAuth = new AuthMiddleware(config.apiSharedSecret);
        JwtAuthMiddleware jwtAuth = new JwtAuthMiddleware(jwtService);
        AuthRoutes mobileAuth = new AuthRoutes(new sv.asociacion.api.auth.AuthService(
            new JdbcUserAuthRepository(), new JdbcSessionRepository()
        ));

        Javalin app = Javalin.create(cfg -> {
            cfg.jetty.host = HOST;
            cfg.jetty.port = config.apiPort;
            cfg.http.maxRequestSize = 268_435_456L;

            cfg.routes.before("/api/updates/*", sharedSecretAuth::authenticate);
            cfg.routes.before("/api/*", jwtAuth::authenticate);

            cfg.routes.get("/health", health::health);
            cfg.routes.post("/api/admin/auth/login", adminAuth::login);
            cfg.routes.get("/api/miembros", miembros::getAll);
            cfg.routes.get("/api/miembros/{id}", miembros::getById);
            cfg.routes.post("/api/miembros", miembros::create);
            cfg.routes.put("/api/miembros/{id}", miembros::update);
            cfg.routes.patch("/api/miembros/{id}/estado", miembros::changeState);
            cfg.routes.get("/api/proyectos", proyectos::getAll);

            cfg.routes.get("/api/viviendas", viviendas::getAll);
            cfg.routes.get("/api/viviendas/{id}", viviendas::getById);
            cfg.routes.post("/api/viviendas", viviendas::create);
            cfg.routes.put("/api/viviendas/{id}", viviendas::update);
            cfg.routes.delete("/api/viviendas/{id}", viviendas::deactivate);

            cfg.routes.get("/api/usuarios", usuarios::getAll);
            cfg.routes.get("/api/usuarios/{id}", usuarios::getById);
            cfg.routes.post("/api/usuarios", usuarios::create);
            cfg.routes.put("/api/usuarios/{id}", usuarios::update);
            cfg.routes.delete("/api/usuarios/{id}", usuarios::delete);

            cfg.routes.get("/api/updates/windows/manifest", updates::manifestV1);
            cfg.routes.get("/api/updates/windows/manifest-v2", updates::manifestV2);
            cfg.routes.get("/api/updates/windows/build-manifest", updates::buildManifest);
            cfg.routes.get("/api/updates/windows/package", updates::legacyPackage);
            cfg.routes.get("/api/updates/windows/delta", updates::deltaPackage);
            cfg.routes.post("/api/updates/windows/publish", updates::publish);

            cfg.routes.get("/api/mobile/updates/android/manifest", androidUpdates::manifest);
            cfg.routes.get("/api/mobile/updates/android/package", androidUpdates::packageFile);
            cfg.routes.post("/api/updates/android/publish", androidUpdates::publish);
            mobileAuth.register(cfg.routes);

            cfg.routes.exception(Exception.class, (exception, context) -> {
                exception.printStackTrace(System.err);
                context.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .json(Map.of("error", "No fue posible procesar la solicitud."));
            });
        });
        app.start();
        System.out.printf("Asociacion API disponible en http://%s:%d%n", HOST, config.apiPort);
    }
}
