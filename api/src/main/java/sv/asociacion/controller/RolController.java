package sv.asociacion.controller;

import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import java.util.Map;
import sv.asociacion.service.RolService;

public class RolController {
    private final RolService rolService;

    public RolController(RolService rolService) { this.rolService = rolService; }

    public void getAll(Context context) {
        String role = context.attribute("role");
        if (!"ADMIN".equalsIgnoreCase(role) && !"ADMINISTRADOR".equalsIgnoreCase(role)) {
            context.status(HttpStatus.FORBIDDEN).json(Map.of("error", "Se requiere rol de administrador."));
            return;
        }
        context.json(rolService.findAll());
    }
}
