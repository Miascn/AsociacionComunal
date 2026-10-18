package sv.asociacion.controller;

import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import sv.asociacion.domain.dto.CreateMemberRequest;
import sv.asociacion.domain.dto.MemberStateRequest;
import sv.asociacion.service.CensoMiembrosService;
import sv.asociacion.service.MiembroService;
import java.util.Map;

/**
 * Rutas del padron de miembros.
 *
 * <p>No captura excepciones: los errores de negocio y de persistencia los traduce a
 * HTTP el manejador central ({@code ApiServer.configurarManejoErrores}). Cada metodo
 * queda reducido a lo que le corresponde a un controlador: leer la peticion, llamar
 * al servicio y elegir el codigo de exito.</p>
 */
public class MiembroController {
    private static final Map<String, String> NO_ENCONTRADO = Map.of("error", "Miembro no encontrado.");

    private final MiembroService miembroService;
    /** Lecturas del padron: depende del contrato generico, no de la implementacion. */
    private final CensoMiembrosService censo;

    public MiembroController(MiembroService miembroService, CensoMiembrosService censo) {
        this.miembroService = miembroService;
        this.censo = censo;
    }

    public void getAll(Context context) {
        context.json(censo.findAll());
    }

    public void getById(Context context) {
        responderOEncontrado(context, censo.findById(id(context)), HttpStatus.OK);
    }

    public void create(Context context) {
        CreateMemberRequest request = context.bodyAsClass(CreateMemberRequest.class);
        context.status(HttpStatus.CREATED).json(miembroService.createMember(request));
    }

    public void update(Context context) {
        var response = miembroService.update(id(context), context.bodyAsClass(CreateMemberRequest.class));
        responderOEncontrado(context, response, HttpStatus.OK);
    }

    public void changeState(Context context) {
        var response = miembroService.changeState(id(context), context.bodyAsClass(MemberStateRequest.class).estado());
        responderOEncontrado(context, response, HttpStatus.OK);
    }

    public void getCredenciales(Context context) {
        var cred = miembroService.getCredenciales(id(context));
        if (cred == null) {
            context.status(HttpStatus.NOT_FOUND).json(Map.of("error", "No se encontraron credenciales o usuario asociado para este miembro."));
        } else {
            context.json(cred);
        }
    }

    public void generarCredenciales(Context context) {
        responderOEncontrado(context, miembroService.generarCredenciales(id(context)), HttpStatus.CREATED);
    }

    private static int id(Context context) {
        return context.pathParamAsClass("id", Integer.class).get();
    }

    private static void responderOEncontrado(Context context, Object body, HttpStatus exito) {
        if (body == null) context.status(HttpStatus.NOT_FOUND).json(NO_ENCONTRADO);
        else context.status(exito).json(body);
    }
}
