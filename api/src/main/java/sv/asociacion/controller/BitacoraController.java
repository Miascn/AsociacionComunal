package sv.asociacion.controller;

import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import java.util.Map;
import sv.asociacion.domain.dto.BitacoraPageResponse;
import sv.asociacion.domain.dto.BitacoraResponse;
import sv.asociacion.service.BitacoraService;

public class BitacoraController {
    private final BitacoraService bitacoraService;

    public BitacoraController(BitacoraService bitacoraService) {
        this.bitacoraService = bitacoraService;
    }

    public void getPage(Context context) {
        if (!hasAuditPermission(context)) {
            context.status(HttpStatus.FORBIDDEN).json(Map.of("error", "Se requiere rol de administrador o síndico para consultar la bitácora."));
            return;
        }

        int page = context.queryParamAsClass("page", Integer.class).getOrDefault(1);
        int size = context.queryParamAsClass("size", Integer.class).getOrDefault(20);
        String usuario = context.queryParam("usuario");
        String entidad = context.queryParam("entidad");
        String accion = context.queryParam("accion");
        String desde = context.queryParam("desde");
        String hasta = context.queryParam("hasta");

        BitacoraPageResponse response = bitacoraService.findPage(usuario, entidad, accion, desde, hasta, page, size);
        context.json(response);
    }

    public void getById(Context context) {
        if (!hasAuditPermission(context)) {
            context.status(HttpStatus.FORBIDDEN).json(Map.of("error", "Se requiere rol de administrador o síndico para consultar la bitácora."));
            return;
        }

        Long id = context.pathParamAsClass("id", Long.class).getOrDefault(null);
        if (id == null) {
            context.status(HttpStatus.BAD_REQUEST).json(Map.of("error", "ID inválido."));
            return;
        }

        BitacoraResponse response = bitacoraService.findById(id);
        if (response == null) {
            context.status(HttpStatus.NOT_FOUND).json(Map.of("error", "Registro de bitácora no encontrado."));
            return;
        }

        context.json(response);
    }

    public static boolean hasAuditPermission(Context context) {
        String role = context.attribute("role");
        if (role == null) return false;
        String r = role.trim().toUpperCase();
        return r.equals("ADMIN") || r.equals("ADMINISTRADOR") || r.equals("SINDICO");
    }
}
