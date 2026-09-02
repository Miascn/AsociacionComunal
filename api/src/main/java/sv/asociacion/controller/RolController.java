package sv.asociacion.controller;

import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import java.util.Map;
import java.util.NoSuchElementException;
import sv.asociacion.domain.dto.RolRequest;
import sv.asociacion.domain.dto.RolResponse;
import sv.asociacion.service.RolService;

public class RolController {
    private final RolService rolService;

    public RolController(RolService rolService) {
        this.rolService = rolService;
    }

    public void getAll(Context context) {
        context.json(rolService.findAll());
    }

    public void getById(Context context) {
        Integer id = context.pathParamAsClass("id", Integer.class).getOrNull();
        if (id == null) {
            context.status(HttpStatus.BAD_REQUEST).json(Map.of("error", "ID inválido."));
            return;
        }
        RolResponse response = rolService.findById(id);
        if (response == null) {
            context.status(HttpStatus.NOT_FOUND).json(Map.of("error", "Rol no encontrado."));
            return;
        }
        context.json(response);
    }

    public void create(Context context) {
        if (!requireAdministrator(context)) return;
        try {
            RolRequest request = context.bodyAsClass(RolRequest.class);
            RolResponse response = rolService.create(request);
            context.status(HttpStatus.CREATED).json(response);
        } catch (IllegalArgumentException e) {
            context.status(HttpStatus.BAD_REQUEST).json(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            context.status(HttpStatus.CONFLICT).json(Map.of("error", e.getMessage()));
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
            RolRequest request = context.bodyAsClass(RolRequest.class);
            RolResponse response = rolService.update(id, request);
            context.json(response);
        } catch (NoSuchElementException e) {
            context.status(HttpStatus.NOT_FOUND).json(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            context.status(HttpStatus.BAD_REQUEST).json(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            context.status(HttpStatus.CONFLICT).json(Map.of("error", e.getMessage()));
        }
    }

    public void delete(Context context) {
        if (!requireAdministrator(context)) return;
        try {
            Integer id = context.pathParamAsClass("id", Integer.class).getOrNull();
            if (id == null) {
                context.status(HttpStatus.BAD_REQUEST).json(Map.of("error", "ID inválido."));
                return;
            }
            boolean deleted = rolService.delete(id);
            if (!deleted) {
                context.status(HttpStatus.NOT_FOUND).json(Map.of("error", "Rol no encontrado."));
                return;
            }
            context.status(HttpStatus.NO_CONTENT);
        } catch (NoSuchElementException e) {
            context.status(HttpStatus.NOT_FOUND).json(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            context.status(HttpStatus.CONFLICT).json(Map.of("error", e.getMessage()));
        }
    }

    private static boolean requireAdministrator(Context context) {
        String role = context.attribute("role");
        if (role == null || (!"ADMIN".equalsIgnoreCase(role) && !"ADMINISTRADOR".equalsIgnoreCase(role))) {
            context.status(HttpStatus.FORBIDDEN).json(Map.of("error", "Se requiere rol de administrador."));
            return false;
        }
        return true;
    }
}
