package sv.asociacion.controller;

import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import java.util.Map;
import sv.asociacion.domain.dto.ViviendaRequest;
import sv.asociacion.service.ViviendaService;

public class ViviendaController {
    private final ViviendaService service;
    public ViviendaController(ViviendaService service) { this.service = service; }

    public void getAll(Context context) { context.json(service.findAll()); }
    public void getById(Context context) {
        var value = service.find(id(context));
        if (value == null) context.status(HttpStatus.NOT_FOUND).json(Map.of("error", "Vivienda no encontrada."));
        else context.json(value);
    }
    public void create(Context context) { save(context, false); }
    public void update(Context context) { save(context, true); }
    public void deactivate(Context context) { service.deactivate(id(context)); context.status(HttpStatus.NO_CONTENT); }

    private void save(Context context, boolean update) {
        try {
            ViviendaRequest request = context.bodyAsClass(ViviendaRequest.class);
            context.status(update ? HttpStatus.OK : HttpStatus.CREATED)
                .json(update ? service.update(id(context), request) : service.create(request));
        } catch (IllegalArgumentException exception) {
            context.status(HttpStatus.BAD_REQUEST).json(Map.of("error", exception.getMessage()));
        }
    }
    private static int id(Context context) { return Integer.parseInt(context.pathParam("id")); }
}
