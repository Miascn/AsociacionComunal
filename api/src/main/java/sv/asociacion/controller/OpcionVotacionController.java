package sv.asociacion.controller;

import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import java.util.Map;
import sv.asociacion.domain.dto.OpcionVotacionRequest;
import sv.asociacion.domain.dto.OpcionVotacionResponse;
import sv.asociacion.domain.dto.ReordenarOpcionesRequest;
import sv.asociacion.service.OpcionVotacionService;

public class OpcionVotacionController {
    private final OpcionVotacionService service;

    public OpcionVotacionController(OpcionVotacionService service) {
        this.service = service;
    }

    public void getByVotacion(Context context) {
        Integer idVotacion = context.pathParamAsClass("idVotacion", Integer.class).getOrDefault(null);
        if (idVotacion == null) {
            context.status(HttpStatus.BAD_REQUEST).json(Map.of("error", "ID de votación inválido."));
            return;
        }

        try {
            context.json(service.findByVotacion(idVotacion));
        } catch (IllegalArgumentException e) {
            context.status(HttpStatus.NOT_FOUND).json(Map.of("error", e.getMessage()));
        }
    }

    public void getById(Context context) {
        Integer id = context.pathParamAsClass("id", Integer.class).getOrDefault(null);
        if (id == null) {
            context.status(HttpStatus.BAD_REQUEST).json(Map.of("error", "ID de opción inválido."));
            return;
        }

        OpcionVotacionResponse res = service.findById(id);
        if (res == null) {
            context.status(HttpStatus.NOT_FOUND).json(Map.of("error", "Opción de votación no encontrada."));
            return;
        }
        context.json(res);
    }

    public void create(Context context) {
        if (!canManage(context)) {
            context.status(HttpStatus.FORBIDDEN).json(Map.of("error", "No tienes permisos para agregar opciones de votación."));
            return;
        }

        Integer idVotacion = context.pathParamAsClass("idVotacion", Integer.class).getOrDefault(null);
        if (idVotacion == null) {
            context.status(HttpStatus.BAD_REQUEST).json(Map.of("error", "ID de votación inválido."));
            return;
        }

        try {
            OpcionVotacionRequest body = context.bodyAsClass(OpcionVotacionRequest.class);
            OpcionVotacionRequest req = new OpcionVotacionRequest(idVotacion, body.descripcion(), body.orden());
            OpcionVotacionResponse created = service.create(req);
            context.status(HttpStatus.CREATED).json(created);
        } catch (IllegalArgumentException e) {
            context.status(HttpStatus.BAD_REQUEST).json(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            context.status(HttpStatus.CONFLICT).json(Map.of("error", e.getMessage()));
        }
    }

    public void update(Context context) {
        if (!canManage(context)) {
            context.status(HttpStatus.FORBIDDEN).json(Map.of("error", "No tienes permisos para editar opciones de votación."));
            return;
        }

        Integer id = context.pathParamAsClass("id", Integer.class).getOrDefault(null);
        if (id == null) {
            context.status(HttpStatus.BAD_REQUEST).json(Map.of("error", "ID de opción inválido."));
            return;
        }

        try {
            OpcionVotacionRequest req = context.bodyAsClass(OpcionVotacionRequest.class);
            OpcionVotacionResponse updated = service.update(id, req);
            context.json(updated);
        } catch (IllegalArgumentException e) {
            context.status(HttpStatus.BAD_REQUEST).json(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            context.status(HttpStatus.CONFLICT).json(Map.of("error", e.getMessage()));
        }
    }

    public void reordenar(Context context) {
        if (!canManage(context)) {
            context.status(HttpStatus.FORBIDDEN).json(Map.of("error", "No tienes permisos para reordenar opciones."));
            return;
        }

        Integer idVotacion = context.pathParamAsClass("idVotacion", Integer.class).getOrDefault(null);
        if (idVotacion == null) {
            context.status(HttpStatus.BAD_REQUEST).json(Map.of("error", "ID de votación inválido."));
            return;
        }

        try {
            ReordenarOpcionesRequest req = context.bodyAsClass(ReordenarOpcionesRequest.class);
            service.reordenar(idVotacion, req.idsEnOrden());
            context.json(service.findByVotacion(idVotacion));
        } catch (IllegalArgumentException e) {
            context.status(HttpStatus.BAD_REQUEST).json(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            context.status(HttpStatus.CONFLICT).json(Map.of("error", e.getMessage()));
        }
    }

    public void delete(Context context) {
        if (!canManage(context)) {
            context.status(HttpStatus.FORBIDDEN).json(Map.of("error", "No tienes permisos para eliminar opciones de votación."));
            return;
        }

        Integer id = context.pathParamAsClass("id", Integer.class).getOrDefault(null);
        if (id == null) {
            context.status(HttpStatus.BAD_REQUEST).json(Map.of("error", "ID de opción inválido."));
            return;
        }

        try {
            boolean ok = service.delete(id);
            if (ok) {
                context.json(Map.of("message", "Opción eliminada correctamente.", "id", id));
            } else {
                context.status(HttpStatus.NOT_FOUND).json(Map.of("error", "Opción no encontrada."));
            }
        } catch (IllegalStateException e) {
            context.status(HttpStatus.CONFLICT).json(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            context.status(HttpStatus.BAD_REQUEST).json(Map.of("error", e.getMessage()));
        }
    }

    private static boolean canManage(Context context) {
        String role = context.attribute("role");
        if (role == null) return false;
        String r = role.trim().toUpperCase();
        return r.equals("ADMIN") || r.equals("ADMINISTRADOR") || r.equals("PRESIDENTE");
    }
}
