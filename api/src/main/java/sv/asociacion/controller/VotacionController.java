package sv.asociacion.controller;

import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import java.util.Map;
import sv.asociacion.domain.dto.VotacionRequest;
import sv.asociacion.domain.dto.VotacionResponse;
import sv.asociacion.domain.entity.Votacion;
import sv.asociacion.service.VotacionService;

public class VotacionController {
    private final VotacionService service;

    public VotacionController(VotacionService service) {
        this.service = service;
    }

    public void getAll(Context context) {
        Votacion.Estado estado = null;
        String estadoParam = context.queryParam("estado");
        if (estadoParam != null && !estadoParam.isBlank() && !"TODOS".equalsIgnoreCase(estadoParam)) {
            try {
                estado = Votacion.Estado.valueOf(estadoParam.trim().toUpperCase());
            } catch (Exception ignored) {}
        }

        Integer idProyecto = null;
        String proyectoParam = context.queryParam("proyectoId");
        if (proyectoParam != null && !proyectoParam.isBlank()) {
            try {
                idProyecto = Integer.parseInt(proyectoParam.trim());
            } catch (Exception ignored) {}
        }

        String busqueda = context.queryParam("busqueda");
        context.json(service.findFiltered(estado, idProyecto, busqueda));
    }

    public void getById(Context context) {
        Integer id = context.pathParamAsClass("id", Integer.class).getOrDefault(null);
        if (id == null) {
            context.status(HttpStatus.BAD_REQUEST).json(Map.of("error", "ID de votación inválido."));
            return;
        }

        VotacionResponse res = service.findById(id);
        if (res == null) {
            context.status(HttpStatus.NOT_FOUND).json(Map.of("error", "Votación no encontrada."));
            return;
        }
        context.json(res);
    }

    public void create(Context context) {
        if (!canManage(context)) {
            context.status(HttpStatus.FORBIDDEN).json(Map.of("error", "No tienes permisos para crear procesos de votación."));
            return;
        }

        try {
            VotacionRequest req = context.bodyAsClass(VotacionRequest.class);
            VotacionResponse created = service.create(req);
            context.status(HttpStatus.CREATED).json(created);
        } catch (IllegalArgumentException e) {
            context.status(HttpStatus.BAD_REQUEST).json(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            context.status(HttpStatus.CONFLICT).json(Map.of("error", e.getMessage()));
        }
    }

    public void update(Context context) {
        if (!canManage(context)) {
            context.status(HttpStatus.FORBIDDEN).json(Map.of("error", "No tienes permisos para editar votaciones."));
            return;
        }

        Integer id = context.pathParamAsClass("id", Integer.class).getOrDefault(null);
        if (id == null) {
            context.status(HttpStatus.BAD_REQUEST).json(Map.of("error", "ID de votación inválido."));
            return;
        }

        try {
            VotacionRequest req = context.bodyAsClass(VotacionRequest.class);
            VotacionResponse updated = service.update(id, req);
            context.json(updated);
        } catch (IllegalArgumentException e) {
            context.status(HttpStatus.BAD_REQUEST).json(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            context.status(HttpStatus.CONFLICT).json(Map.of("error", e.getMessage()));
        }
    }

    public void abrir(Context context) {
        if (!canManage(context)) {
            context.status(HttpStatus.FORBIDDEN).json(Map.of("error", "No tienes permisos para abrir votaciones."));
            return;
        }

        Integer id = context.pathParamAsClass("id", Integer.class).getOrDefault(null);
        if (id == null) {
            context.status(HttpStatus.BAD_REQUEST).json(Map.of("error", "ID de votación inválido."));
            return;
        }

        try {
            VotacionResponse res = service.abrir(id);
            context.json(res);
        } catch (IllegalArgumentException | IllegalStateException e) {
            context.status(HttpStatus.BAD_REQUEST).json(Map.of("error", e.getMessage()));
        }
    }

    public void cerrar(Context context) {
        if (!canManage(context)) {
            context.status(HttpStatus.FORBIDDEN).json(Map.of("error", "No tienes permisos para cerrar votaciones."));
            return;
        }

        Integer id = context.pathParamAsClass("id", Integer.class).getOrDefault(null);
        if (id == null) {
            context.status(HttpStatus.BAD_REQUEST).json(Map.of("error", "ID de votación inválido."));
            return;
        }

        try {
            VotacionResponse res = service.cerrar(id);
            context.json(res);
        } catch (IllegalArgumentException | IllegalStateException e) {
            context.status(HttpStatus.BAD_REQUEST).json(Map.of("error", e.getMessage()));
        }
    }

    public void cancelar(Context context) {
        if (!canManage(context)) {
            context.status(HttpStatus.FORBIDDEN).json(Map.of("error", "No tienes permisos para cancelar votaciones."));
            return;
        }

        Integer id = context.pathParamAsClass("id", Integer.class).getOrDefault(null);
        if (id == null) {
            context.status(HttpStatus.BAD_REQUEST).json(Map.of("error", "ID de votación inválido."));
            return;
        }

        try {
            VotacionResponse res = service.cancelar(id);
            context.json(res);
        } catch (IllegalArgumentException | IllegalStateException e) {
            context.status(HttpStatus.BAD_REQUEST).json(Map.of("error", e.getMessage()));
        }
    }

    public void delete(Context context) {
        if (!canManage(context)) {
            context.status(HttpStatus.FORBIDDEN).json(Map.of("error", "No tienes permisos para eliminar votaciones."));
            return;
        }

        Integer id = context.pathParamAsClass("id", Integer.class).getOrDefault(null);
        if (id == null) {
            context.status(HttpStatus.BAD_REQUEST).json(Map.of("error", "ID de votación inválido."));
            return;
        }

        try {
            boolean ok = service.delete(id);
            if (ok) {
                context.json(Map.of("message", "Votación eliminada correctamente.", "id", id));
            } else {
                context.status(HttpStatus.NOT_FOUND).json(Map.of("error", "Votación no encontrada."));
            }
        } catch (IllegalStateException e) {
            context.status(HttpStatus.CONFLICT).json(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            context.status(HttpStatus.BAD_REQUEST).json(Map.of("error", e.getMessage()));
        }
    }

    public static boolean canManage(Context context) {
        String role = context.attribute("role");
        if (role == null) return false;
        String r = role.trim().toUpperCase();
        return r.equals("ADMIN") || r.equals("ADMINISTRADOR") || r.equals("PRESIDENTE");
    }
}
