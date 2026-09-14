package sv.asociacion.service;

import java.time.LocalDate;
import java.util.List;
import sv.asociacion.dao.ViviendaDAO;
import sv.asociacion.domain.dto.*;
import sv.asociacion.domain.entity.Vivienda;

public class ViviendaService {
    private final ViviendaDAO dao;

    public ViviendaService(ViviendaDAO dao) { this.dao = dao; }

    public List<ViviendaResponse> findAll() {
        return dao.findAll().stream().map(ViviendaResponse::from).toList();
    }

    public ViviendaDetailResponse find(int id) {
        Vivienda value = dao.findById(id).orElse(null);
        if (value == null) return null;
        var residents = dao.residents(id).stream()
            .map(r -> new ResidentResponse(
                r.getIdResidente(), r.getIdMiembro(), r.getNombreCompleto(),
                r.getTipoPersona() == null ? null : r.getTipoPersona().name(), r.isRepresentante()))
            .toList();
        return new ViviendaDetailResponse(ViviendaResponse.from(value), residents);
    }

    public ViviendaResponse create(ViviendaRequest request) { return save(null, request); }
    public ViviendaResponse update(int id, ViviendaRequest request) { return save(id, request); }
    public void deactivate(int id) { dao.deactivate(id); }

    private ViviendaResponse save(Integer id, ViviendaRequest request) {
        String error = request.validationError();
        if (error != null) throw new IllegalArgumentException(error);
        Vivienda value = new Vivienda(
            id, request.codigo().trim().toUpperCase(), request.sector().trim(), request.direccion().trim(),
            clean(request.referencia()), request.idRepresentante(), null, LocalDate.now(), request.estado(), 0, 0
        );
        List<String> adults = request.adultos() == null ? List.of() : request.adultos();
        List<String> minors = request.menores() == null ? List.of() : request.menores();
        return ViviendaResponse.from(id == null ? dao.create(value, adults, minors) : dao.update(value, adults, minors));
    }

    private static String clean(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
