package sv.asociacion.service;

import java.util.List;
import sv.asociacion.api.auth.MemberProvisioningService;
import sv.asociacion.dao.MiembroDAO;
import sv.asociacion.domain.dto.CreateMemberRequest;
import sv.asociacion.domain.dto.CreateMemberResponse;
import sv.asociacion.domain.dto.MiembroResponse;
import sv.asociacion.domain.entity.Miembro;

public class MiembroService {
    private final MiembroDAO miembroDAO;
    private final MemberProvisioningService provisioning;

    public MiembroService(MiembroDAO miembroDAO, MemberProvisioningService provisioning) {
        this.miembroDAO = miembroDAO;
        this.provisioning = provisioning;
    }

    public List<MiembroResponse> findAll() {
        return miembroDAO.findAll().stream().map(MiembroResponse::from).toList();
    }

    public MiembroResponse findById(int id) {
        return miembroDAO.findById(id).map(MiembroResponse::from).orElse(null);
    }

    public CreateMemberResponse createMember(CreateMemberRequest request) {
        String error = request.validationError();
        if (error != null) throw new IllegalArgumentException(error);
        String document = request.documento().trim();
        if (miembroDAO.existsByDui(document)) throw new IllegalStateException("Ya existe un miembro con ese documento.");
        MemberProvisioningService.Result created = provisioning.create(
            document, request.tipoDocumento().trim(), clean(request.paisOrigen()), request.idVivienda(),
            request.nombres().trim(), request.apellidos().trim(), clean(request.telefono()), clean(request.correo())
        );
        var member = miembroDAO.findById(created.memberId())
            .orElseThrow(() -> new IllegalStateException("El miembro creado no pudo recuperarse."));
        return new CreateMemberResponse(MiembroResponse.from(member), document, created.temporaryPassword());
    }

    public MiembroResponse update(int id, CreateMemberRequest request) {
        String error = request.validationError();
        if (error != null) throw new IllegalArgumentException(error);
        Miembro member = miembroDAO.findById(id).orElse(null);
        if (member == null) return null;
        String document = request.documento().trim();
        if (miembroDAO.existsByDuiExcludingId(document, id)) {
            throw new IllegalStateException("Ya existe otro miembro con ese documento.");
        }
        member.setDui(document);
        member.setTipoDocumento(request.tipoDocumento().trim());
        member.setPaisOrigen(clean(request.paisOrigen()));
        member.setIdVivienda(request.idVivienda());
        member.setNombres(request.nombres().trim());
        member.setApellidos(request.apellidos().trim());
        member.setTelefono(clean(request.telefono()));
        member.setCorreo(clean(request.correo()));
        Miembro updated = miembroDAO.update(member);
        return updated == null ? null : MiembroResponse.from(updated);
    }

    public MiembroResponse changeState(int id, String state) {
        Miembro.Estado estado;
        try {
            estado = Miembro.Estado.valueOf(state == null ? "" : state.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("El estado debe ser ACTIVO o INACTIVO.");
        }
        if (!miembroDAO.changeEstado(id, estado)) return null;
        return miembroDAO.findById(id).map(MiembroResponse::from).orElse(null);
    }

    private static String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
