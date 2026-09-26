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
        if (!canManage(context)) {
            Integer sessionMemberId = context.attribute("idMiembro");
            if (sessionMemberId != null) {
                idMiembro = sessionMemberId;
            }
        }
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
        boolean isManager = canManage(context);
        Integer sessionMemberId = context.attribute("idMiembro");

        if (!isManager && sessionMemberId == null) {
            context.status(HttpStatus.FORBIDDEN).json(Map.of("error", "Se requiere rol de Administrador, Tesorero o Residente registrado para realizar aportaciones."));
            return;
        }

        try {
            AportacionRequest request = context.bodyAsClass(AportacionRequest.class);
            if (!isManager) {
                // Seguridad: Los pagos desde celular se asocian estrictamente al residente en sesión
                request = new AportacionRequest(
                    sessionMemberId,
                    request.idProyecto(),
                    request.periodoMes(),
                    request.monto(),
                    request.fechaPago() != null ? request.fechaPago() : java.time.LocalDate.now(),
                    request.metodoPago() != null ? request.metodoPago() : sv.asociacion.domain.entity.Aportacion.MetodoPago.TRANSFERENCIA,
                    request.referencia() != null && !request.referencia().isBlank() ? request.referencia() : "Pago móvil - Vigilancia mensual"
                );
            }
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

    public void ajustarMonto(Context context) {
        if (!canManage(context)) {
            context.status(HttpStatus.FORBIDDEN).json(Map.of("error", "Se requiere rol de Administrador o Tesorero para ajustar aportaciones."));
            return;
        }

        Long id = context.pathParamAsClass("id", Long.class).getOrNull();
        if (id == null) {
            context.status(HttpStatus.BAD_REQUEST).json(Map.of("error", "ID de aportación inválido."));
            return;
        }

        try {
            Map<?, ?> body = context.bodyAsClass(Map.class);
            Object montoObj = body != null ? body.get("monto") : null;
            if (montoObj == null) {
                context.status(HttpStatus.BAD_REQUEST).json(Map.of("error", "El campo 'monto' es obligatorio para ajustar la aportación."));
                return;
            }
            java.math.BigDecimal nuevoMonto = new java.math.BigDecimal(montoObj.toString().trim());
            AportacionResponse updated = aportacionService.ajustarMonto(id, nuevoMonto);
            context.json(updated);
        } catch (NumberFormatException e) {
            context.status(HttpStatus.BAD_REQUEST).json(Map.of("error", "El monto ingresado no es un número válido."));
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

    public void getConfiguracion(Context context) {
        if (!canView(context)) {
            context.status(HttpStatus.FORBIDDEN).json(Map.of("error", "No tienes permisos para consultar la configuración."));
            return;
        }
        java.math.BigDecimal cuota = aportacionService.getCuotaMantenimiento();
        context.json(new sv.asociacion.domain.dto.ConfiguracionCuotaDto(
            cuota, "Cuota mensual de mantenimiento y vigilancia de la colonia"
        ));
    }

    public void updateConfiguracion(Context context) {
        if (!canManage(context)) {
            context.status(HttpStatus.FORBIDDEN).json(Map.of("error", "Se requiere rol de Administrador o Tesorero para modificar la cuota de mantenimiento."));
            return;
        }
        try {
            sv.asociacion.domain.dto.ConfiguracionCuotaDto req = context.bodyAsClass(sv.asociacion.domain.dto.ConfiguracionCuotaDto.class);
            if (req.cuotaMantenimiento() == null || req.cuotaMantenimiento().compareTo(java.math.BigDecimal.ZERO) <= 0) {
                context.status(HttpStatus.BAD_REQUEST).json(Map.of("error", "La cuota debe ser un valor positivo mayor a cero."));
                return;
            }
            aportacionService.setCuotaMantenimiento(req.cuotaMantenimiento(), req.descripcion());
            context.json(Map.of(
                "message", "Cuota de mantenimiento actualizada correctamente.",
                "cuotaMantenimiento", req.cuotaMantenimiento()
            ));
        } catch (Exception e) {
            context.status(HttpStatus.BAD_REQUEST).json(Map.of("error", e.getMessage()));
        }
    }

    public void getMantenimientoPeriodo(Context context) {
        if (!canView(context)) {
            context.status(HttpStatus.FORBIDDEN).json(Map.of("error", "No tienes permisos para consultar el mantenimiento."));
            return;
        }
        String periodo = context.queryParam("periodo");
        String busqueda = context.queryParam("busqueda");
        var res = aportacionService.getMantenimientoPeriodo(periodo, busqueda);
        context.json(res);
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
