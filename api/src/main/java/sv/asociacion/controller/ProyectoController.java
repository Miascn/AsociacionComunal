package sv.asociacion.controller;

import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import java.util.Map;
import sv.asociacion.domain.dto.CambioEstadoProyectoRequest;
import sv.asociacion.domain.dto.ProyectoRequest;
import sv.asociacion.domain.dto.ProyectoResponse;
import sv.asociacion.domain.entity.Proyecto;
import sv.asociacion.service.ProyectoService;

public class ProyectoController {
    private final ProyectoService proyectoService;

    public ProyectoController(ProyectoService proyectoService) {
        this.proyectoService = proyectoService;
    }

    public void getAll(Context context) {
        String estadoParam = context.queryParam("estado");
        Proyecto.Estado estado = null;
        if (estadoParam != null && !estadoParam.isBlank() && !"TODOS".equalsIgnoreCase(estadoParam)) {
            try {
                estado = Proyecto.Estado.valueOf(estadoParam.trim().toUpperCase());
            } catch (Exception ignored) {}
        }
        String busqueda = context.queryParam("busqueda");
        Integer creadoPor = context.queryParamAsClass("creadoPor", Integer.class).getOrNull();

        context.json(proyectoService.findFiltered(estado, creadoPor, busqueda));
    }

    public void getById(Context context) {
        Integer id = context.pathParamAsClass("id", Integer.class).getOrNull();
        if (id == null) {
            context.status(HttpStatus.BAD_REQUEST).json(Map.of("error", "ID inválido."));
            return;
        }

        ProyectoResponse response = proyectoService.findById(id);
        if (response == null) {
            context.status(HttpStatus.NOT_FOUND).json(Map.of("error", "Proyecto no encontrado."));
            return;
        }

        context.json(response);
    }

    public void create(Context context) {
        if (!canManage(context)) {
            context.status(HttpStatus.FORBIDDEN).json(Map.of("error", "No tienes permisos para crear proyectos comunales."));
            return;
        }

        try {
            ProyectoRequest request = context.bodyAsClass(ProyectoRequest.class);
            Integer currentUserId = context.attribute("idUsuario");
            if (request.creadoPor() == null && currentUserId != null) {
                request = new ProyectoRequest(
                    request.nombre(), request.descripcion(), request.presupuesto(), currentUserId, request.estado()
                );
            }
            ProyectoResponse created = proyectoService.create(request);
            context.status(HttpStatus.CREATED).json(created);
        } catch (IllegalArgumentException e) {
            context.status(HttpStatus.BAD_REQUEST).json(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            context.status(HttpStatus.CONFLICT).json(Map.of("error", e.getMessage()));
        }
    }

    public void update(Context context) {
        if (!canManage(context)) {
            context.status(HttpStatus.FORBIDDEN).json(Map.of("error", "No tienes permisos para editar proyectos comunales."));
            return;
        }

        Integer id = context.pathParamAsClass("id", Integer.class).getOrNull();
        if (id == null) {
            context.status(HttpStatus.BAD_REQUEST).json(Map.of("error", "ID inválido."));
            return;
        }

        try {
            ProyectoRequest request = context.bodyAsClass(ProyectoRequest.class);
            ProyectoResponse updated = proyectoService.update(id, request);
            context.json(updated);
        } catch (IllegalArgumentException e) {
            context.status(HttpStatus.BAD_REQUEST).json(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            context.status(HttpStatus.CONFLICT).json(Map.of("error", e.getMessage()));
        }
    }

    public void changeState(Context context) {
        if (!canManage(context)) {
            context.status(HttpStatus.FORBIDDEN).json(Map.of("error", "No tienes permisos para cambiar el estado de proyectos."));
            return;
        }

        Integer id = context.pathParamAsClass("id", Integer.class).getOrNull();
        if (id == null) {
            context.status(HttpStatus.BAD_REQUEST).json(Map.of("error", "ID inválido."));
            return;
        }

        try {
            CambioEstadoProyectoRequest request = context.bodyAsClass(CambioEstadoProyectoRequest.class);
            if (request.nuevoEstado() == null) {
                context.status(HttpStatus.BAD_REQUEST).json(Map.of("error", "El nuevo estado es requerido."));
                return;
            }
            ProyectoResponse updated = proyectoService.cambiarEstado(id, request.nuevoEstado());
            context.json(updated);
        } catch (IllegalArgumentException | IllegalStateException e) {
            context.status(HttpStatus.BAD_REQUEST).json(Map.of("error", e.getMessage()));
        }
    }

    public void delete(Context context) {
        if (!canManage(context)) {
            context.status(HttpStatus.FORBIDDEN).json(Map.of("error", "No tienes permisos para eliminar proyectos comunales."));
            return;
        }

        Integer id = context.pathParamAsClass("id", Integer.class).getOrNull();
        if (id == null) {
            context.status(HttpStatus.BAD_REQUEST).json(Map.of("error", "ID inválido."));
            return;
        }

        try {
            boolean ok = proyectoService.delete(id);
            if (ok) {
                context.json(Map.of("message", "Proyecto eliminado correctamente.", "id", id));
            } else {
                context.status(HttpStatus.NOT_FOUND).json(Map.of("error", "Proyecto no encontrado."));
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
        return r.equals("ADMIN") || r.equals("ADMINISTRADOR") || r.equals("PRESIDENTE") || r.equals("TESORERO");
    }
}