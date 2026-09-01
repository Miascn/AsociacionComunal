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
        BitacoraDAO bitacoraDAO = new BitacoraDAO();
        AportacionDAO aportacionDAO = new AportacionDAO();
        CargoDAO cargoDAO = new CargoDAO();
        MiembroCargoDAO miembroCargoDAO = new MiembroCargoDAO();
        PeriodoDirectivaDAO periodoDAO = new PeriodoDirectivaDAO();
        VotacionDAO votacionDAO = new VotacionDAO();
        OpcionVotacionDAO opcionVotacionDAO = new OpcionVotacionDAO();
        VotoDAO votoDAO = new VotoDAO();
        ReunionDAO reunionDAO = new ReunionDAO();
        AsistenciaDAO asistenciaDAO = new AsistenciaDAO();

        sv.asociacion.service.AuthService jwtService =
            new sv.asociacion.service.AuthService(usuarioDAO, rolDAO, miembroDAO, config.jwtSecret);
        MiembroService miembroService = new MiembroService(miembroDAO, new MemberProvisioningService());
        ProyectoService proyectoService = new ProyectoService(proyectoDAO, usuarioDAO, aportacionDAO);
        UsuarioService usuarioService = new UsuarioService(usuarioDAO, rolDAO, miembroDAO);
        RolService rolService = new RolService(rolDAO, usuarioDAO);
        ViviendaService viviendaService = new ViviendaService(viviendaDAO);
        BitacoraService bitacoraService = new BitacoraService(bitacoraDAO);
        AportacionService aportacionService = new AportacionService(aportacionDAO, miembroDAO, proyectoDAO);
        CargoService cargoService = new CargoService(cargoDAO, miembroCargoDAO);
        PeriodoService periodoService = new PeriodoService(periodoDAO, miembroCargoDAO);
        AsignacionCargoService asignacionService = new AsignacionCargoService(miembroCargoDAO, miembroDAO, cargoDAO, periodoDAO);
        VotacionService votacionService = new VotacionService(votacionDAO, opcionVotacionDAO, votoDAO, proyectoDAO);
        OpcionVotacionService opcionVotacionService = new OpcionVotacionService(opcionVotacionDAO, votacionDAO, votoDAO);
        VotoService votoService = new VotoService(votoDAO, votacionDAO, opcionVotacionDAO, miembroDAO);
        ReunionService reunionService = new ReunionService(reunionDAO, asistenciaDAO);

        AuthController adminAuth = new AuthController(jwtService);
        HealthController health = new HealthController();
        MiembroController miembros = new MiembroController(miembroService);
        ProyectoController proyectos = new ProyectoController(proyectoService);
        UsuarioController usuarios = new UsuarioController(usuarioService);
        RolController roles = new RolController(rolService);
        ViviendaController viviendas = new ViviendaController(viviendaService);
        BitacoraController bitacoras = new BitacoraController(bitacoraService);
        AportacionController aportaciones = new AportacionController(aportacionService);
        CargoController cargos = new CargoController(cargoService);
        PeriodoController periodos = new PeriodoController(periodoService);
        AsignacionCargoController asignaciones = new AsignacionCargoController(asignacionService);
        VotacionController votaciones = new VotacionController(votacionService);
        OpcionVotacionController opcionesVotacion = new OpcionVotacionController(opcionVotacionService);
        VotoController votos = new VotoController(votoService, usuarioDAO);
        ReunionController reuniones = new ReunionController(reunionService);
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
            cfg.routes.get("/api/proyectos/{id}", proyectos::getById);
            cfg.routes.post("/api/proyectos", proyectos::create);
            cfg.routes.put("/api/proyectos/{id}", proyectos::update);
            cfg.routes.patch("/api/proyectos/{id}/estado", proyectos::changeState);
            cfg.routes.delete("/api/proyectos/{id}", proyectos::delete);

            cfg.routes.get("/api/viviendas", viviendas::getAll);
            cfg.routes.get("/api/viviendas/{id}", viviendas::getById);
            cfg.routes.post("/api/viviendas", viviendas::create);
            cfg.routes.put("/api/viviendas/{id}", viviendas::update);
            cfg.routes.delete("/api/viviendas/{id}", viviendas::deactivate);

            cfg.routes.get("/api/usuarios", usuarios::getAll);
            cfg.routes.get("/api/roles", roles::getAll);
            cfg.routes.get("/api/roles/{id}", roles::getById);
            cfg.routes.post("/api/roles", roles::create);
            cfg.routes.put("/api/roles/{id}", roles::update);
            cfg.routes.delete("/api/roles/{id}", roles::delete);
            cfg.routes.get("/api/usuarios/{id}", usuarios::getById);
            cfg.routes.post("/api/usuarios", usuarios::create);
            cfg.routes.put("/api/usuarios/{id}", usuarios::update);
            cfg.routes.patch("/api/usuarios/{id}/estado", usuarios::changeState);
            cfg.routes.delete("/api/usuarios/{id}", usuarios::delete);

            cfg.routes.get("/api/bitacoras", bitacoras::getPage);
            cfg.routes.get("/api/bitacoras/{id}", bitacoras::getById);

            cfg.routes.get("/api/aportaciones", aportaciones::getAll);
            cfg.routes.get("/api/aportaciones/{id}", aportaciones::getById);
            cfg.routes.post("/api/aportaciones", aportaciones::create);
            cfg.routes.put("/api/aportaciones/{id}", aportaciones::update);
            cfg.routes.patch("/api/aportaciones/{id}/anular", aportaciones::anular);

            cfg.routes.get("/api/cargos", cargos::getAll);
            cfg.routes.get("/api/cargos/{id}", cargos::getById);
            cfg.routes.post("/api/cargos", cargos::create);
            cfg.routes.put("/api/cargos/{id}", cargos::update);
            cfg.routes.patch("/api/cargos/{id}/desactivar", cargos::toggleActivo);
            cfg.routes.delete("/api/cargos/{id}", cargos::delete);

            cfg.routes.get("/api/periodos", periodos::getAll);
            cfg.routes.get("/api/periodos/activo", periodos::getActivo);
            cfg.routes.get("/api/periodos/{id}", periodos::getById);
            cfg.routes.post("/api/periodos", periodos::create);
            cfg.routes.put("/api/periodos/{id}", periodos::update);
            cfg.routes.patch("/api/periodos/{id}/activar", periodos::activar);
            cfg.routes.patch("/api/periodos/{id}/finalizar", periodos::finalizar);
            cfg.routes.delete("/api/periodos/{id}", periodos::delete);

            cfg.routes.get("/api/directiva/actual", asignaciones::getDirectivaActual);
            cfg.routes.get("/api/directiva/periodo/{id}", asignaciones::getDirectivaPeriodo);
            cfg.routes.get("/api/asignaciones-cargo", asignaciones::getAll);
            cfg.routes.get("/api/asignaciones-cargo/{id}", asignaciones::getById);
            cfg.routes.post("/api/asignaciones-cargo", asignaciones::create);
            cfg.routes.patch("/api/asignaciones-cargo/{id}/revocar", asignaciones::revocar);
            cfg.routes.patch("/api/asignaciones-cargo/{id}/finalizar", asignaciones::finalizar);
            cfg.routes.delete("/api/asignaciones-cargo/{id}", asignaciones::delete);

            cfg.routes.get("/api/votaciones", votaciones::getAll);
            cfg.routes.get("/api/votaciones/{id}", votaciones::getById);
            cfg.routes.post("/api/votaciones", votaciones::create);
            cfg.routes.put("/api/votaciones/{id}", votaciones::update);
            cfg.routes.patch("/api/votaciones/{id}/abrir", votaciones::abrir);
            cfg.routes.patch("/api/votaciones/{id}/cerrar", votaciones::cerrar);
            cfg.routes.patch("/api/votaciones/{id}/cancelar", votaciones::cancelar);
            cfg.routes.delete("/api/votaciones/{id}", votaciones::delete);

            cfg.routes.get("/api/votaciones/{idVotacion}/opciones", opcionesVotacion::getByVotacion);
            cfg.routes.post("/api/votaciones/{idVotacion}/opciones", opcionesVotacion::create);
            cfg.routes.patch("/api/votaciones/{idVotacion}/opciones/reordenar", opcionesVotacion::reordenar);
            cfg.routes.get("/api/opciones-votacion/{id}", opcionesVotacion::getById);
            cfg.routes.put("/api/opciones-votacion/{id}", opcionesVotacion::update);
            cfg.routes.delete("/api/opciones-votacion/{id}", opcionesVotacion::delete);

            cfg.routes.post("/api/votos", votos::emitir);
            cfg.routes.get("/api/votaciones/{idVotacion}/mi-participacion", votos::verificarParticipacion);

            cfg.routes.get("/api/reuniones", reuniones::getAll);
            cfg.routes.get("/api/reuniones/{id}", reuniones::getById);
            cfg.routes.post("/api/reuniones", reuniones::create);
            cfg.routes.put("/api/reuniones/{id}", reuniones::update);
            cfg.routes.patch("/api/reuniones/{id}/realizada", reuniones::marcarRealizada);
            cfg.routes.patch("/api/reuniones/{id}/cancelar", reuniones::cancelar);
            cfg.routes.delete("/api/reuniones/{id}", reuniones::delete);

            cfg.routes.after("/api/*", ctx -> {
                int status = ctx.status().getCode();
                if (status >= 200 && status < 300) {
                    String method = ctx.method().name();
                    String path = ctx.path();
                    if (path.startsWith("/api/bitacoras")) return;

                    if (method.equals("POST") || method.equals("PUT") || method.equals("PATCH") || method.equals("DELETE")) {
                        Integer idUsuario = ctx.attribute("idUsuario");
                        String entidad = extractEntity(path);
                        String accion = extractAction(method, path);
                        String idRegistro = extractId(path);
                        bitacoraService.registrar(idUsuario, accion, entidad, idRegistro, method + " " + path);
                    }
                }
            });

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

    private static String extractEntity(String path) {
        if (path == null) return "SISTEMA";
        if (path.contains("/usuarios")) return "USUARIO";
        if (path.contains("/roles")) return "ROL";
        if (path.contains("/miembros")) return "MIEMBRO";
        if (path.contains("/viviendas")) return "VIVIENDA";
        if (path.contains("/proyectos")) return "PROYECTO";
        if (path.contains("/aportaciones")) return "APORTACION";
        if (path.contains("/cargos")) return "CARGO";
        if (path.contains("/periodos")) return "PERIODO";
        if (path.contains("/directiva") || path.contains("/asignaciones-cargo")) return "ASIGNACION_DIRECTIVA";
        if (path.contains("/opciones")) return "OPCION_VOTACION";
        if (path.contains("/votos")) return "VOTO";
        if (path.contains("/votaciones")) return "VOTACION";
        if (path.contains("/reuniones")) return "REUNION";
        if (path.contains("/auth") || path.contains("/login")) return "AUTH";
        return "GENERAL";
    }

    private static String extractAction(String method, String path) {
        if (path != null && path.contains("/anular")) return "ANULAR";
        if (path != null && path.contains("/desactivar")) return "TOGGLE_ACTIVE";
        if (path != null && path.contains("/activar")) return "ACTIVAR";
        if (path != null && path.contains("/finalizar")) return "FINALIZAR";
        if (path != null && path.contains("/revocar")) return "REVOCAR";
        if (path != null && path.contains("/abrir")) return "ABRIR";
        if (path != null && path.contains("/cerrar")) return "CERRAR";
        if (path != null && path.contains("/cancelar")) return "CANCELAR";
        if (path != null && path.contains("/realizada")) return "REALIZADA";
        if (path != null && path.contains("/reordenar")) return "REORDENAR";
        if (path != null && path.contains("/estado")) return "STATE_CHANGE";
        if (path != null && path.contains("/login")) return "LOGIN";
        return switch (method) {
            case "POST" -> "CREATE";
            case "PUT", "PATCH" -> "UPDATE";
            case "DELETE" -> "DELETE";
            default -> method;
        };
    }

    private static String extractId(String path) {
        if (path == null) return null;
        String[] parts = path.split("/");
        for (int i = parts.length - 1; i >= 0; i--) {
            if (parts[i].matches("\\d+")) {
                return parts[i];
            }
        }
        return null;
    }
}

