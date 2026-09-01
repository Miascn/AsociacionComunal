package sv.asociacion.controller;

import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import java.util.Map;
import sv.asociacion.domain.dto.AsignacionCargoRequest;
import sv.asociacion.domain.dto.AsignacionCargoResponse;
import sv.asociacion.domain.dto.RevocarAsignacionRequest;
import sv.asociacion.service.AsignacionCargoService;

public class AsignacionCargoController {
    private final AsignacionCargoService service;

    public AsignacionCargoController(AsignacionCargoService service) {
        this.service = service;
    }

    public void getDirectivaActual(Context context) {
        context.json(service.findDirectivaActual());
    }

    public void getDirectivaPeriodo(Context context) {
        Integer idPeriodo = context.pathParamAsClass("id", Integer.class).getOrDefault(null);
        if (idPeriodo == null) {
            context.status(HttpStatus.BAD_REQUEST).json(Map.of("error", "ID de período inválido."));
            return;
        }
        context.json(service.findDirectivaPeriodo(idPeriodo));
    }

    public void getAll(Context context) {
        Integer idPeriodo = null;
        String periodoParam = context.queryParam("periodoId");
        if (periodoParam != null && !periodoParam.isBlank()) {
            try {
                idPeriodo = Integer.parseInt(periodoParam.trim());
            } catch (Exception ignored) {}
        }
        String estado = context.queryParam("estado");
        String busqueda = context.queryParam("busqueda");
        context.json(service.findFiltered(idPeriodo, estado, busqueda));
    }

    public void getById(Context context) {
        Integer id = context.pathParamAsClass("id", Integer.class).getOrDefault(null);
        if (id == null) {
            context.status(HttpStatus.BAD_REQUEST).json(Map.of("error", "ID de asignación inválido."));
            return;
        }

        AsignacionCargoResponse res = service.findById(id);
        if (res == null) {
            context.status(HttpStatus.NOT_FOUND).json(Map.of("error", "Asignación no encontrada."));
            return;
        }
        context.json(res);
    }

    public void create(Context context) {
        if (!canManage(context)) {
            context.status(HttpStatus.FORBIDDEN).json(Map.of("error", "No tienes permisos para asignar cargos directivos."));
            return;
        }

        try {
            AsignacionCargoRequest req = context.bodyAsClass(AsignacionCargoRequest.class);
            AsignacionCargoResponse created = service.create(req);
            context.status(HttpStatus.CREATED).json(created);
        } catch (IllegalArgumentException e) {
            context.status(HttpStatus.BAD_REQUEST).json(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            context.status(HttpStatus.CONFLICT).json(Map.of("error", e.getMessage()));
        }
    }

    public void revocar(Context context) {
        if (!canManage(context)) {
            context.status(HttpStatus.FORBIDDEN).json(Map.of("error", "No tienes permisos para revocar cargos directivos."));
            return;
        }

        Integer id = context.pathParamAsClass("id", Integer.class).getOrDefault(null);
        if (id == null) {
            context.status(HttpStatus.BAD_REQUEST).json(Map.of("error", "ID de asignación inválido."));
            return;
        }

        try {
            RevocarAsignacionRequest req = null;
            if (context.body() != null && !context.body().isBlank()) {
                req = context.bodyAsClass(RevocarAsignacionRequest.class);
            }
            AsignacionCargoResponse res = service.revocar(id, req);
            context.json(res);
        } catch (IllegalArgumentException | IllegalStateException e) {
            context.status(HttpStatus.BAD_REQUEST).json(Map.of("error", e.getMessage()));
        }
    }

    public void finalizar(Context context) {
        if (!canManage(context)) {
            context.status(HttpStatus.FORBIDDEN).json(Map.of("error", "No tienes permisos para finalizar asignaciones directivas."));
            return;
        }

        Integer id = context.pathParamAsClass("id", Integer.class).getOrDefault(null);
        if (id == null) {
            context.status(HttpStatus.BAD_REQUEST).json(Map.of("error", "ID de asignación inválido."));
            return;
        }

        try {
            AsignacionCargoResponse res = service.finalizar(id);
            context.json(res);
        } catch (IllegalArgumentException | IllegalStateException e) {
            context.status(HttpStatus.BAD_REQUEST).json(Map.of("error", e.getMessage()));
        }
    }

    public void delete(Context context) {
        if (!canManage(context)) {
            context.status(HttpStatus.FORBIDDEN).json(Map.of("error", "No tienes permisos para eliminar asignaciones directivas."));
            return;
        }

        Integer id = context.pathParamAsClass("id", Integer.class).getOrDefault(null);
        if (id == null) {
            context.status(HttpStatus.BAD_REQUEST).json(Map.of("error", "ID de asignación inválido."));
            return;
        }

        try {
            boolean ok = service.delete(id);
            if (ok) {
                context.json(Map.of("message", "Asignación eliminada correctamente.", "id", id));
            } else {
                context.status(HttpStatus.NOT_FOUND).json(Map.of("error", "Asignación no encontrada."));
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
