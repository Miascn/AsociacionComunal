package sv.asociacion.controller;

import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import java.util.Map;
import sv.asociacion.dao.UsuarioDAO;
import sv.asociacion.domain.dto.EmitirVotoRequest;
import sv.asociacion.domain.dto.EmitirVotoResponse;
import sv.asociacion.domain.dto.EstadoParticipacionResponse;
import sv.asociacion.domain.entity.Usuario;
import sv.asociacion.service.VotoService;

public class VotoController {
    private final VotoService service;
    private final UsuarioDAO usuarioDAO;

    public VotoController(VotoService service) {
        this(service, null);
    }

    public VotoController(VotoService service, UsuarioDAO usuarioDAO) {
        this.service = service;
        this.usuarioDAO = usuarioDAO;
    }

    public void emitir(Context context) {
        try {
            EmitirVotoRequest req = context.bodyAsClass(EmitirVotoRequest.class);

            Integer idMiembroSesion = null;
            Integer idUsuario = context.attribute("idUsuario");
            if (idUsuario != null && usuarioDAO != null) {
                Usuario u = usuarioDAO.findById(idUsuario);
                if (u != null) {
                    idMiembroSesion = u.getIdMiembro();
                }
            }

            EmitirVotoResponse res = service.emitirVoto(req, idMiembroSesion);
            context.status(HttpStatus.CREATED).json(res);
        } catch (IllegalArgumentException e) {
            context.status(HttpStatus.BAD_REQUEST).json(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            context.status(HttpStatus.CONFLICT).json(Map.of("error", e.getMessage()));
        }
    }

    public void verificarParticipacion(Context context) {
        Integer idVotacion = context.pathParamAsClass("idVotacion", Integer.class).getOrDefault(null);
        if (idVotacion == null) {
            context.status(HttpStatus.BAD_REQUEST).json(Map.of("error", "ID de votación inválido."));
            return;
        }

        Integer idMiembro = null;
        String mParam = context.queryParam("miembroId");
        if (mParam != null && !mParam.isBlank()) {
            try {
                idMiembro = Integer.parseInt(mParam.trim());
            } catch (Exception ignored) {}
        }
        if (idMiembro == null) {
            Integer idUsuario = context.attribute("idUsuario");
            if (idUsuario != null && usuarioDAO != null) {
                Usuario u = usuarioDAO.findById(idUsuario);
                if (u != null) {
                    idMiembro = u.getIdMiembro();
                }
            }
        }

        if (idMiembro == null) {
            context.status(HttpStatus.BAD_REQUEST).json(Map.of("error", "ID de miembro no proporcionado ni deducible del usuario en sesión."));
            return;
        }

        EstadoParticipacionResponse res = service.verificarParticipacion(idVotacion, idMiembro);
        context.json(res);
    }
}
