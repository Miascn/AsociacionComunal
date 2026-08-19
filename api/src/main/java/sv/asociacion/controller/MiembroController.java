package sv.asociacion.controller;

import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import sv.asociacion.domain.dto.CreateMemberRequest;
import sv.asociacion.service.MiembroService;
import java.util.Map;

public class MiembroController {
    private final MiembroService miembroService;

    public MiembroController(MiembroService miembroService) {
        this.miembroService = miembroService;
    }

    public void getAll(Context context) {
        context.json(miembroService.findAll());
    }

    public void create(Context context) {
        CreateMemberRequest request = context.bodyAsClass(CreateMemberRequest.class);
        try {
            var response = miembroService.createMember(request);
            context.status(HttpStatus.CREATED).json(response);
        } catch (IllegalArgumentException e) {
            context.status(HttpStatus.BAD_REQUEST).json(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            context.status(HttpStatus.CONFLICT).json(Map.of("error", e.getMessage()));
        }
    }
}