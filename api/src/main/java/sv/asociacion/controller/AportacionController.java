package sv.asociacion.controller;

import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import java.util.Map;
import sv.asociacion.domain.dto.AportacionPageResponse;
import sv.asociacion.domain.dto.AportacionRequest;
import sv.asociacion.domain.dto.AportacionResponse;
import sv.asociacion.service.AportacionService;

public class AportacionController {
    private final AportacionService aportacionService;

    public AportacionController(AportacionService aportacionService) {
        this.aportacionService = aportacionService;
    }

    public void getAll(Context context) {
        if (!canView(context)) {
            context.status(HttpStatus.FORBIDDEN).json(Map.of("error", "No tienes permisos para consultar las aportaciones."));
            return;
        }

        Integer idMiembro = context.queryParamAsClass("idMiembro", Integer.class).getOrNull();
        Integer idProyecto = context.queryParamAsClass("idProyecto", Integer.class).getOrNull();
        String periodo = context.queryParam("periodo");
        String desde = context.queryParam("desde");
        String hasta = context.queryParam("hasta");
        String metodo = context.queryParam("metodo");
        String estado = context.queryParam("estado");
        String busqueda = context.queryParam("busqueda");
        int page = context.queryParamAsClass("page", Integer.class).getOrDefault(1);
        int size = context.queryParamAsClass("size", Integer.class).getOrDefault(25);

        AportacionPageResponse response = aportacionService.findPage(
            idMiembro, idProyecto, periodo, desde, hasta, metodo, estado, busqueda, page, size
        );
        context.json(response);
    }

    public void getById(Context context) {
        if (!canView(context)) {
            context.status(HttpStatus.FORBIDDEN).json(Map.of("error", "No tienes permisos para consultar esta aportación."));
            return;
        }

        Long id = context.pathParamAsClass("id", Long.class).getOrNull();
        if (id == null) {
            context.status(HttpStatus.BAD_REQUEST).json(Map.of("error", "ID de aportación inválido."));
            return;
        }

        AportacionResponse response = aportacionService.findById(id);
        if (response == null) {
            context.status(HttpStatus.NOT_FOUND).json(Map.of("error", "Aportación no encontrada."));
            return;
        }

        context.json(response);
    }

    public void create(Context context) {
        if (!canManage(context)) {
            context.status(HttpStatus.FORBIDDEN).json(Map.of("error", "Se requiere rol de Administrador o Tesorero para registrar aportaciones."));
            return;
        }

        try {
            AportacionRequest request = context.bodyAsClass(AportacionRequest.class);
            AportacionResponse created = aportacionService.create(request);
            context.status(HttpStatus.CREATED).json(created);
        } catch (IllegalArgumentException e) {
            context.status(HttpStatus.BAD_REQUEST).json(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            context.status(HttpStatus.CONFLICT).json(Map.of("error", e.getMessage()));
        }
    }

    public void update(Context context) {
        if (!canManage(context)) {
            context.status(HttpStatus.FORBIDDEN).json(Map.of("error", "Se requiere rol de Administrador o Tesorero para editar aportaciones."));
            return;
        }

        Long id = context.pathParamAsClass("id", Long.class).getOrNull();
        if (id == null) {
            context.status(HttpStatus.BAD_REQUEST).json(Map.of("error", "ID de aportación inválido."));
            return;
        }

        try {
            AportacionRequest request = context.bodyAsClass(AportacionRequest.class);
            AportacionResponse updated = aportacionService.update(id, request);
            context.json(updated);
        } catch (IllegalArgumentException e) {
            context.status(HttpStatus.BAD_REQUEST).json(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            context.status(HttpStatus.CONFLICT).json(Map.of("error", e.getMessage()));
        }
    }

    public void anular(Context context) {
        if (!canManage(context)) {
            context.status(HttpStatus.FORBIDDEN).json(Map.of("error", "Se requiere rol de Administrador o Tesorero para anular aportaciones."));
            return;
        }

        Long id = context.pathParamAsClass("id", Long.class).getOrNull();
        if (id == null) {
            context.status(HttpStatus.BAD_REQUEST).json(Map.of("error", "ID de aportación inválido."));
            return;
        }

        try {
            boolean ok = aportacionService.anular(id);
            if (ok) {
                context.json(Map.of("message", "Aportación anulada correctamente.", "id", id));
            } else {
                context.status(HttpStatus.NOT_FOUND).json(Map.of("error", "Aportación no encontrada."));
            }
        } catch (Exception e) {
            context.status(HttpStatus.BAD_REQUEST).json(Map.of("error", e.getMessage()));
        }
    }

    public static boolean canManage(Context context) {
        String role = context.attribute("role");
        if (role == null) return false;
        String r = role.trim().toUpperCase();
        return r.equals("ADMIN") || r.equals("ADMINISTRADOR") || r.equals("TESORERO");
    }

    public static boolean canView(Context context) {
        String role = context.attribute("role");
        if (role == null) return false;
        String r = role.trim().toUpperCase();
        return canManage(context) || r.equals("PRESIDENTE") || r.equals("SINDICO") || r.equals("MIEMBRO");
    }
}
