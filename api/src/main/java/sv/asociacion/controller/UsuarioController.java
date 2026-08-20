package sv.asociacion.controller;

import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import sv.asociacion.domain.dto.CreateUsuarioRequest;
import sv.asociacion.domain.dto.UpdateUsuarioRequest;
import sv.asociacion.service.UsuarioService;
import java.util.Map;

public class UsuarioController {
    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    public void getAll(Context context) {
        if (!requireAdministrator(context)) return;
        context.json(usuarioService.findAll());
    }

    public void getById(Context context) {
        if (!requireAdministrator(context)) return;
        Integer id = context.pathParamAsClass("id", Integer.class).getOrDefault(null);
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
        if (request.nombreUsuario() == null || request.nombreUsuario().isBlank()
            || request.clave() == null || request.clave().isBlank()) {
            context.status(HttpStatus.BAD_REQUEST).json(Map.of("error", "Nombre de usuario y clave son obligatorios."));
            return;
        }
        try {
            var response = usuarioService.create(request);
            context.status(HttpStatus.CREATED).json(response);
        } catch (IllegalStateException e) {
            context.status(HttpStatus.CONFLICT).json(Map.of("error", e.getMessage()));
        }
    }

    public void update(Context context) {
        if (!requireAdministrator(context)) return;
        Integer id = context.pathParamAsClass("id", Integer.class).getOrDefault(null);
        if (id == null) {
            context.status(HttpStatus.BAD_REQUEST).json(Map.of("error", "ID inválido."));
            return;
        }
        UpdateUsuarioRequest request = context.bodyAsClass(UpdateUsuarioRequest.class);
        try {
            var response = usuarioService.update(id, request);
            context.json(response);
        } catch (IllegalArgumentException e) {
            context.status(HttpStatus.NOT_FOUND).json(Map.of("error", e.getMessage()));
        }
    }

    public void delete(Context context) {
        if (!requireAdministrator(context)) return;
        Integer id = context.pathParamAsClass("id", Integer.class).getOrDefault(null);
        if (id == null) {
            context.status(HttpStatus.BAD_REQUEST).json(Map.of("error", "ID inválido."));
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
        if ("ADMIN".equals(role) || "ADMINISTRADOR".equals(role)) return true;
        context.status(HttpStatus.FORBIDDEN).json(Map.of("error", "Se requiere rol de administrador."));
        return false;
    }
}
