package sv.asociacion.controller;

import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import java.util.Map;
import sv.asociacion.domain.dto.CargoRequest;
import sv.asociacion.domain.dto.CargoResponse;
import sv.asociacion.service.CargoService;

public class CargoController {
    private final CargoService cargoService;

    public CargoController(CargoService cargoService) {
        this.cargoService = cargoService;
    }

    public void getAll(Context context) {
        Boolean activo = context.queryParamAsClass("activo", Boolean.class).getOrDefault(null);
        String busqueda = context.queryParam("busqueda");
        context.json(cargoService.findFiltered(activo, busqueda));
    }

    public void getById(Context context) {
        Integer id = context.pathParamAsClass("id", Integer.class).getOrDefault(null);
        if (id == null) {
            context.status(HttpStatus.BAD_REQUEST).json(Map.of("error", "ID de cargo inválido."));
            return;
        }

        CargoResponse response = cargoService.findById(id);
        if (response == null) {
            context.status(HttpStatus.NOT_FOUND).json(Map.of("error", "Cargo directivo no encontrado."));
            return;
        }

        context.json(response);
    }

    public void create(Context context) {
        if (!canManage(context)) {
            context.status(HttpStatus.FORBIDDEN).json(Map.of("error", "No tienes permisos para crear cargos directivos."));
            return;
        }

        try {
            CargoRequest request = context.bodyAsClass(CargoRequest.class);
            CargoResponse created = cargoService.create(request);
            context.status(HttpStatus.CREATED).json(created);
        } catch (IllegalArgumentException e) {
            context.status(HttpStatus.BAD_REQUEST).json(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            context.status(HttpStatus.CONFLICT).json(Map.of("error", e.getMessage()));
        }
    }

    public void update(Context context) {
        if (!canManage(context)) {
            context.status(HttpStatus.FORBIDDEN).json(Map.of("error", "No tienes permisos para editar cargos directivos."));
            return;
        }

        Integer id = context.pathParamAsClass("id", Integer.class).getOrDefault(null);
        if (id == null) {
            context.status(HttpStatus.BAD_REQUEST).json(Map.of("error", "ID de cargo inválido."));
            return;
        }

        try {
            CargoRequest request = context.bodyAsClass(CargoRequest.class);
            CargoResponse updated = cargoService.update(id, request);
            context.json(updated);
        } catch (IllegalArgumentException e) {
            context.status(HttpStatus.BAD_REQUEST).json(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            context.status(HttpStatus.CONFLICT).json(Map.of("error", e.getMessage()));
        }
    }

    public void toggleActivo(Context context) {
        if (!canManage(context)) {
            context.status(HttpStatus.FORBIDDEN).json(Map.of("error", "No tienes permisos para modificar el estado de cargos directivos."));
            return;
        }

        Integer id = context.pathParamAsClass("id", Integer.class).getOrDefault(null);
        if (id == null) {
            context.status(HttpStatus.BAD_REQUEST).json(Map.of("error", "ID de cargo inválido."));
            return;
        }

        try {
            boolean activo = context.queryParamAsClass("activo", Boolean.class).getOrDefault(true);
            CargoResponse updated = cargoService.toggleActivo(id, activo);
            context.json(updated);
        } catch (IllegalArgumentException e) {
            context.status(HttpStatus.BAD_REQUEST).json(Map.of("error", e.getMessage()));
        }
    }

    public void delete(Context context) {
        if (!canManage(context)) {
            context.status(HttpStatus.FORBIDDEN).json(Map.of("error", "No tienes permisos para eliminar cargos directivos."));
            return;
        }

        Integer id = context.pathParamAsClass("id", Integer.class).getOrDefault(null);
        if (id == null) {
            context.status(HttpStatus.BAD_REQUEST).json(Map.of("error", "ID de cargo inválido."));
            return;
        }

        try {
            boolean ok = cargoService.delete(id);
            if (ok) {
                context.json(Map.of("message", "Cargo directivo eliminado correctamente.", "id", id));
            } else {
                context.status(HttpStatus.NOT_FOUND).json(Map.of("error", "Cargo directivo no encontrado."));
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
