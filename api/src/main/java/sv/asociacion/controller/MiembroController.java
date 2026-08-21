package sv.asociacion.controller;

import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import sv.asociacion.domain.dto.CreateMemberRequest;
import sv.asociacion.domain.dto.MemberStateRequest;
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

    public void getById(Context context) {
        var response = miembroService.findById(context.pathParamAsClass("id", Integer.class).get());
        if (response == null) context.status(HttpStatus.NOT_FOUND).json(Map.of("error", "Miembro no encontrado."));
        else context.json(response);
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

    public void update(Context context) {
        try {
            int id = context.pathParamAsClass("id", Integer.class).get();
            var response = miembroService.update(id, context.bodyAsClass(CreateMemberRequest.class));
            if (response == null) context.status(HttpStatus.NOT_FOUND).json(Map.of("error", "Miembro no encontrado."));
            else context.json(response);
        } catch (IllegalArgumentException e) {
            context.status(HttpStatus.BAD_REQUEST).json(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            context.status(HttpStatus.CONFLICT).json(Map.of("error", e.getMessage()));
        }
    }

    public void changeState(Context context) {
        try {
            int id = context.pathParamAsClass("id", Integer.class).get();
            var response = miembroService.changeState(id, context.bodyAsClass(MemberStateRequest.class).estado());
            if (response == null) context.status(HttpStatus.NOT_FOUND).json(Map.of("error", "Miembro no encontrado."));
            else context.json(response);
        } catch (IllegalArgumentException e) {
            context.status(HttpStatus.BAD_REQUEST).json(Map.of("error", e.getMessage()));
        }
    }
}
