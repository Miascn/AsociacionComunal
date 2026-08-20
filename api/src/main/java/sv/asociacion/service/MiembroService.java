package sv.asociacion.service;

import java.util.List;
import sv.asociacion.api.auth.MemberProvisioningService;
import sv.asociacion.dao.MiembroDAO;
import sv.asociacion.domain.dto.CreateMemberRequest;
import sv.asociacion.domain.dto.CreateMemberResponse;
import sv.asociacion.domain.dto.MiembroResponse;

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

    private static String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
