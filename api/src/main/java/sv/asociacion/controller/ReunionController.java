package sv.asociacion.controller;

import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import java.util.Map;
import sv.asociacion.domain.dto.ReunionRequest;
import sv.asociacion.service.ReunionService;

public class ReunionController {
    private final ReunionService service;

    public ReunionController(ReunionService service) {
        this.service = service;
    }

    public void getAll(Context context) {
        String search = context.queryParam("search");
        String tipo = context.queryParam("tipo");
        String estado = context.queryParam("estado");
        String desde = context.queryParam("desde");
        String hasta = context.queryParam("hasta");
        context.json(service.getAll(search, tipo, estado, desde, hasta));
    }

    public void getById(Context context) {
        Integer id = context.pathParamAsClass("id", Integer.class).getOrNull();
        if (id == null) {
            context.status(HttpStatus.BAD_REQUEST).json(Map.of("error", "ID inválido."));
            return;
        }
        try {
            context.json(service.getById(id));
        } catch (IllegalArgumentException e) {
            context.status(HttpStatus.NOT_FOUND).json(Map.of("error", e.getMessage()));
        }
    }

    public void create(Context context) {
        try {
            ReunionRequest req = context.bodyAsClass(ReunionRequest.class);
            context.status(HttpStatus.CREATED).json(service.create(req));
        } catch (IllegalArgumentException e) {
            context.status(HttpStatus.BAD_REQUEST).json(Map.of("error", e.getMessage()));
        }
    }

    public void update(Context context) {
        Integer id = context.pathParamAsClass("id", Integer.class).getOrNull();
        if (id == null) {
            context.status(HttpStatus.BAD_REQUEST).json(Map.of("error", "ID inválido."));
            return;
        }
        try {
            ReunionRequest req = context.bodyAsClass(ReunionRequest.class);
            context.json(service.update(id, req));
        } catch (IllegalArgumentException e) {
            context.status(HttpStatus.BAD_REQUEST).json(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            context.status(HttpStatus.CONFLICT).json(Map.of("error", e.getMessage()));
        }
    }

    public void marcarRealizada(Context context) {
        Integer id = context.pathParamAsClass("id", Integer.class).getOrNull();
        if (id == null) {
            context.status(HttpStatus.BAD_REQUEST).json(Map.of("error", "ID inválido."));
            return;
        }
        try {
            context.json(service.marcarRealizada(id));
        } catch (IllegalArgumentException e) {
            context.status(HttpStatus.NOT_FOUND).json(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            context.status(HttpStatus.CONFLICT).json(Map.of("error", e.getMessage()));
        }
    }

    public void cancelar(Context context) {
        Integer id = context.pathParamAsClass("id", Integer.class).getOrNull();
        if (id == null) {
            context.status(HttpStatus.BAD_REQUEST).json(Map.of("error", "ID inválido."));
            return;
        }
        try {
            context.json(service.cancelar(id));
        } catch (IllegalArgumentException e) {
            context.status(HttpStatus.NOT_FOUND).json(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            context.status(HttpStatus.CONFLICT).json(Map.of("error", e.getMessage()));
        }
    }

    public void delete(Context context) {
        Integer id = context.pathParamAsClass("id", Integer.class).getOrNull();
        if (id == null) {
            context.status(HttpStatus.BAD_REQUEST).json(Map.of("error", "ID inválido."));
            return;
        }
        try {
            service.delete(id);
            context.status(HttpStatus.OK).json(Map.of("message", "Reunión eliminada exitosamente."));
        } catch (IllegalArgumentException e) {
            context.status(HttpStatus.NOT_FOUND).json(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            context.status(HttpStatus.CONFLICT).json(Map.of("error", e.getMessage()));
        }
    }
}
