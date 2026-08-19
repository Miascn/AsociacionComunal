package sv.asociacion.service;

import sv.asociacion.domain.dto.CreateMemberRequest;
import sv.asociacion.domain.dto.MiembroResponse;
import sv.asociacion.dao.MiembroDAO;
import sv.asociacion.domain.entity.Miembro;
import java.time.LocalDate;
import java.util.List;

public class MiembroService {
    private final MiembroDAO miembroDAO;

    public MiembroService(MiembroDAO miembroDAO) {
        this.miembroDAO = miembroDAO;
    }

    public List<MiembroResponse> findAll() {
        return miembroDAO.findAll().stream().map(MiembroResponse::from).toList();
    }

    public MiembroResponse createMember(CreateMemberRequest request) {
        String validationError = request.validationError();
        if (validationError != null) {
            throw new IllegalArgumentException(validationError);
        }
        if (miembroDAO.existsByDui(request.dui().trim())) {
            throw new IllegalStateException("Ya existe un miembro con ese DUI.");
        }
        Miembro miembro = new Miembro(
            null,
            request.dui().trim(),
            request.nombres().trim(),
            request.apellidos().trim(),
            clean(request.telefono()),
            clean(request.correo()),
            request.direccion().trim(),
            LocalDate.now(),
            Miembro.Estado.ACTIVO
        );
        miembroDAO.save(miembro);
        return MiembroResponse.from(miembro);
    }

    private static String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}