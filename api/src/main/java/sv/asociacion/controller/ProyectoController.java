package sv.asociacion.controller;

import io.javalin.http.Context;
import sv.asociacion.service.ProyectoService;

public class ProyectoController {
    private final ProyectoService proyectoService;

    public ProyectoController(ProyectoService proyectoService) {
        this.proyectoService = proyectoService;
    }

    public void getAll(Context context) {
        context.json(proyectoService.findAll());
    }
}