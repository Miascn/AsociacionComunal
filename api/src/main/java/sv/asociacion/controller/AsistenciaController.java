package sv.asociacion.controller;

import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import java.util.Map;
import sv.asociacion.domain.dto.AsistenciaRequest;
import sv.asociacion.domain.dto.ConvocatoriaMasivaRequest;
import sv.asociacion.service.AsistenciaService;

public class AsistenciaController {
    private final AsistenciaService service;

    public AsistenciaController(AsistenciaService service) {
        this.service = service;
    }

    public void getByReunion(Context context) {
        Integer idReunion = context.pathParamAsClass("idReunion", Integer.class).getOrNull();
        if (idReunion == null) {
            context.status(HttpStatus.BAD_REQUEST).json(Map.of("error", "ID de reunión inválido."));
            return;
        }
        try {
            context.json(service.getByReunion(idReunion));
        } catch (IllegalArgumentException e) {
            context.status(HttpStatus.NOT_FOUND).json(Map.of("error", e.getMessage()));
        }
    }

    public void convocar(Context context) {
        Integer idReunion = context.pathParamAsClass("idReunion", Integer.class).getOrNull();
        if (idReunion == null) {
            context.status(HttpStatus.BAD_REQUEST).json(Map.of("error", "ID de reunión inválido."));
            return;
        }
        try {
            ConvocatoriaMasivaRequest req = null;
            if (context.body() != null && !context.body().isBlank()) {
                try {
                    req = context.bodyAsClass(ConvocatoriaMasivaRequest.class);
                } catch (Exception ignored) {}
            }
            context.status(HttpStatus.CREATED).json(service.convocarMiembros(idReunion, req));
        } catch (IllegalArgumentException e) {
            context.status(HttpStatus.BAD_REQUEST).json(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            context.status(HttpStatus.CONFLICT).json(Map.of("error", e.getMessage()));
        }
    }

    public void registrarOActualizar(Context context) {
        Integer idReunion = context.pathParamAsClass("idReunion", Integer.class).getOrNull();
        if (idReunion == null) {
            context.status(HttpStatus.BAD_REQUEST).json(Map.of("error", "ID de reunión inválido."));
            return;
        }
        try {
            AsistenciaRequest req = context.bodyAsClass(AsistenciaRequest.class);
            context.json(service.registrarOActualizar(idReunion, req));
        } catch (IllegalArgumentException e) {
            context.status(HttpStatus.BAD_REQUEST).json(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            context.status(HttpStatus.CONFLICT).json(Map.of("error", e.getMessage()));
        }
    }

    public void registrarLote(Context context) {
        Integer idReunion = context.pathParamAsClass("idReunion", Integer.class).getOrNull();
        if (idReunion == null) {
            context.status(HttpStatus.BAD_REQUEST).json(Map.of("error", "ID de reunión inválido."));
            return;
        }
        try {
            java.util.List<AsistenciaRequest> lote;
            String body = context.body() != null ? context.body().trim() : "";
            if (body.startsWith("[")) {
                lote = java.util.Arrays.asList(context.bodyAsClass(AsistenciaRequest[].class));
            } else {
                sv.asociacion.domain.dto.AsistenciaLoteRequest loteReq = context.bodyAsClass(sv.asociacion.domain.dto.AsistenciaLoteRequest.class);
                lote = loteReq != null ? loteReq.asistencias() : null;
            }
            context.json(service.registrarLote(idReunion, lote));
        } catch (IllegalArgumentException e) {
            if (e.getMessage() != null && e.getMessage().contains("Reunión no encontrada")) {
                context.status(HttpStatus.NOT_FOUND).json(Map.of("error", e.getMessage()));
            } else {
                context.status(HttpStatus.BAD_REQUEST).json(Map.of("error", e.getMessage()));
            }
        } catch (IllegalStateException e) {
            context.status(HttpStatus.CONFLICT).json(Map.of("error", e.getMessage()));
        }
    }

    public void toggle(Context context) {
        Long id = context.pathParamAsClass("id", Long.class).getOrNull();
        if (id == null) {
            context.status(HttpStatus.BAD_REQUEST).json(Map.of("error", "ID de asistencia inválido."));
            return;
        }
        try {
            Map<?, ?> body = context.bodyAsClass(Map.class);
            boolean asistio = Boolean.parseBoolean(String.valueOf(body.get("asistio")));
            String observacion = body.get("observacion") != null ? body.get("observacion").toString() : null;

            context.json(service.toggleAsistencia(id, asistio, observacion));
        } catch (IllegalArgumentException e) {
            context.status(HttpStatus.BAD_REQUEST).json(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            context.status(HttpStatus.CONFLICT).json(Map.of("error", e.getMessage()));
        }
    }

    public void delete(Context context) {
        Long id = context.pathParamAsClass("id", Long.class).getOrNull();
        if (id == null) {
            context.status(HttpStatus.BAD_REQUEST).json(Map.of("error", "ID de asistencia inválido."));
            return;
        }
        try {
            service.delete(id);
            context.status(HttpStatus.OK).json(Map.of("message", "Registro de asistencia eliminado."));
        } catch (IllegalArgumentException e) {
            context.status(HttpStatus.NOT_FOUND).json(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            context.status(HttpStatus.CONFLICT).json(Map.of("error", e.getMessage()));
        }
    }
}
