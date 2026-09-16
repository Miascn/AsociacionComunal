package sv.asociacion.controller;

import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import sv.asociacion.domain.dto.CreateUsuarioRequest;
import sv.asociacion.domain.dto.UpdateUsuarioRequest;
import sv.asociacion.domain.dto.UserStateRequest;
import sv.asociacion.service.UsuarioService;
import java.util.Map;
import java.util.NoSuchElementException;

public class UsuarioController {
    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    public void getAll(Context context) {
        if (!requireAdministrator(context)) return;
        String rol = context.queryParam("rol");
        String estado = context.queryParam("estado");
        context.json(usuarioService.findAll(rol, estado));
    }

    public void getById(Context context) {
        if (!requireAdministrator(context)) return;
        Integer id = context.pathParamAsClass("id", Integer.class).getOrNull();
        if (id == null) {
            context.status(HttpStatus.BAD_REQUEST).json(Map.of("error", "ID inválido."));
            return;
        }
        var response = usuarioService.findById(id);
        if (response == null) {
            context.status(HttpStatus.NOT_FOUND).json(Map.of("error", "Usuario no encontrado."));
            return;
        }
        context.json(response);
    }

    public void create(Context context) {
        if (!requireAdministrator(context)) return;
        CreateUsuarioRequest request = context.bodyAsClass(CreateUsuarioRequest.class);
        try {
            var response = usuarioService.create(request);
            context.status(HttpStatus.CREATED).json(response);
        } catch (IllegalStateException e) {
            context.status(HttpStatus.CONFLICT).json(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            context.status(HttpStatus.BAD_REQUEST).json(Map.of("error", e.getMessage()));
        }
    }

    public void update(Context context) {
        if (!requireAdministrator(context)) return;
        try {
            Integer id = context.pathParamAsClass("id", Integer.class).getOrNull();
            if (id == null) {
                context.status(HttpStatus.BAD_REQUEST).json(Map.of("error", "ID inválido."));
                return;
            }
            UpdateUsuarioRequest request = context.bodyAsClass(UpdateUsuarioRequest.class);
            Integer currentUser = context.attribute("idUsuario");
            if (id.equals(currentUser) && request.estado() != null && !"ACTIVO".equalsIgnoreCase(request.estado())) {
                context.status(HttpStatus.CONFLICT).json(Map.of("error", "No puedes desactivar o bloquear tu propia cuenta."));
                return;
            }
            var response = usuarioService.update(id, request);
            context.json(response);
        } catch (NoSuchElementException e) {
            context.status(HttpStatus.NOT_FOUND).json(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            context.status(HttpStatus.CONFLICT).json(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            context.status(HttpStatus.BAD_REQUEST).json(Map.of("error", e.getMessage()));
        }
    }

    public void changeState(Context context) {
        if (!requireAdministrator(context)) return;
        Integer id = context.pathParamAsClass("id", Integer.class).getOrNull();
        if (id == null) {
            context.status(HttpStatus.BAD_REQUEST).json(Map.of("error", "ID inválido."));
            return;
        }
        UserStateRequest request = context.bodyAsClass(UserStateRequest.class);
        Integer currentUser = context.attribute("idUsuario");
        if (id.equals(currentUser) && !"ACTIVO".equalsIgnoreCase(request.estado())) {
            context.status(HttpStatus.CONFLICT).json(Map.of("error", "No puedes desactivar o bloquear tu propia cuenta."));
            return;
        }
        try {
            context.json(usuarioService.changeState(id, request.estado()));
        } catch (NoSuchElementException e) {
            context.status(HttpStatus.NOT_FOUND).json(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            context.status(HttpStatus.BAD_REQUEST).json(Map.of("error", e.getMessage()));
        }
    }

    public void delete(Context context) {
        if (!requireAdministrator(context)) return;
        Integer id = context.pathParamAsClass("id", Integer.class).getOrNull();
        if (id == null) {
            context.status(HttpStatus.BAD_REQUEST).json(Map.of("error", "ID inválido."));
            return;
        }
        Integer currentUser = context.attribute("idUsuario");
        if (id.equals(currentUser)) {
            context.status(HttpStatus.CONFLICT).json(Map.of("error", "No puedes desactivar tu propia cuenta."));
            return;
        }
        boolean deleted = usuarioService.delete(id);
        if (!deleted) {
            context.status(HttpStatus.NOT_FOUND).json(Map.of("error", "Usuario no encontrado."));
            return;
        }
        context.status(HttpStatus.NO_CONTENT);
    }

    private static boolean requireAdministrator(Context context) {
        String role = context.attribute("role");
        if ("ADMIN".equalsIgnoreCase(role) || "ADMINISTRADOR".equalsIgnoreCase(role)) return true;
        context.status(HttpStatus.FORBIDDEN).json(Map.of("error", "Se requiere rol de administrador."));
        return false;
    }
}
