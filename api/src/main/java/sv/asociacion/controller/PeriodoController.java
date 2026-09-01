package sv.asociacion.controller;

import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import java.util.Map;
import sv.asociacion.domain.dto.PeriodoRequest;
import sv.asociacion.domain.dto.PeriodoResponse;
import sv.asociacion.domain.entity.PeriodoDirectiva;
import sv.asociacion.service.PeriodoService;

public class PeriodoController {
    private final PeriodoService periodoService;

    public PeriodoController(PeriodoService periodoService) {
        this.periodoService = periodoService;
    }

    public void getAll(Context context) {
        String estadoParam = context.queryParam("estado");
        PeriodoDirectiva.Estado estado = null;
        if (estadoParam != null && !estadoParam.isBlank() && !"TODOS".equalsIgnoreCase(estadoParam)) {
            try {
                estado = PeriodoDirectiva.Estado.valueOf(estadoParam.trim().toUpperCase());
            } catch (Exception ignored) {}
        }
        String busqueda = context.queryParam("busqueda");
        context.json(periodoService.findFiltered(estado, busqueda));
    }

    public void getActivo(Context context) {
        PeriodoResponse activo = periodoService.findActivo();
        if (activo == null) {
            context.status(HttpStatus.NOT_FOUND).json(Map.of("message", "No hay ningún período directivo activo actualmente."));
            return;
        }
        context.json(activo);
    }

    public void getById(Context context) {
        Integer id = context.pathParamAsClass("id", Integer.class).getOrDefault(null);
        if (id == null) {
            context.status(HttpStatus.BAD_REQUEST).json(Map.of("error", "ID de período inválido."));
            return;
        }

        PeriodoResponse response = periodoService.findById(id);
        if (response == null) {
            context.status(HttpStatus.NOT_FOUND).json(Map.of("error", "Período directivo no encontrado."));
            return;
        }

        context.json(response);
    }

    public void create(Context context) {
        if (!canManage(context)) {
            context.status(HttpStatus.FORBIDDEN).json(Map.of("error", "No tienes permisos para crear períodos directivos."));
            return;
        }

        try {
            PeriodoRequest request = context.bodyAsClass(PeriodoRequest.class);
            PeriodoResponse created = periodoService.create(request);
            context.status(HttpStatus.CREATED).json(created);
        } catch (IllegalArgumentException e) {
            context.status(HttpStatus.BAD_REQUEST).json(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            context.status(HttpStatus.CONFLICT).json(Map.of("error", e.getMessage()));
        }
    }

    public void update(Context context) {
        if (!canManage(context)) {
            context.status(HttpStatus.FORBIDDEN).json(Map.of("error", "No tienes permisos para editar períodos directivos."));
            return;
        }

        Integer id = context.pathParamAsClass("id", Integer.class).getOrDefault(null);
        if (id == null) {
            context.status(HttpStatus.BAD_REQUEST).json(Map.of("error", "ID de período inválido."));
            return;
        }

        try {
            PeriodoRequest request = context.bodyAsClass(PeriodoRequest.class);
            PeriodoResponse updated = periodoService.update(id, request);
            context.json(updated);
        } catch (IllegalArgumentException e) {
            context.status(HttpStatus.BAD_REQUEST).json(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            context.status(HttpStatus.CONFLICT).json(Map.of("error", e.getMessage()));
        }
    }

    public void activar(Context context) {
        if (!canManage(context)) {
            context.status(HttpStatus.FORBIDDEN).json(Map.of("error", "No tienes permisos para activar períodos directivos."));
            return;
        }

        Integer id = context.pathParamAsClass("id", Integer.class).getOrDefault(null);
        if (id == null) {
            context.status(HttpStatus.BAD_REQUEST).json(Map.of("error", "ID de período inválido."));
            return;
        }

        try {
            PeriodoResponse updated = periodoService.activar(id);
            context.json(updated);
        } catch (IllegalArgumentException | IllegalStateException e) {
            context.status(HttpStatus.BAD_REQUEST).json(Map.of("error", e.getMessage()));
        }
    }

    public void finalizar(Context context) {
        if (!canManage(context)) {
            context.status(HttpStatus.FORBIDDEN).json(Map.of("error", "No tienes permisos para finalizar períodos directivos."));
            return;
        }

        Integer id = context.pathParamAsClass("id", Integer.class).getOrDefault(null);
        if (id == null) {
            context.status(HttpStatus.BAD_REQUEST).json(Map.of("error", "ID de período inválido."));
            return;
        }

        try {
            PeriodoResponse updated = periodoService.finalizar(id);
            context.json(updated);
        } catch (IllegalArgumentException | IllegalStateException e) {
            context.status(HttpStatus.BAD_REQUEST).json(Map.of("error", e.getMessage()));
        }
    }

    public void delete(Context context) {
        if (!canManage(context)) {
            context.status(HttpStatus.FORBIDDEN).json(Map.of("error", "No tienes permisos para eliminar períodos directivos."));
            return;
        }

        Integer id = context.pathParamAsClass("id", Integer.class).getOrDefault(null);
        if (id == null) {
            context.status(HttpStatus.BAD_REQUEST).json(Map.of("error", "ID de período inválido."));
            return;
        }

        try {
            boolean ok = periodoService.delete(id);
            if (ok) {
                context.json(Map.of("message", "Período directivo eliminado correctamente.", "id", id));
            } else {
                context.status(HttpStatus.NOT_FOUND).json(Map.of("error", "Período directivo no encontrado."));
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
