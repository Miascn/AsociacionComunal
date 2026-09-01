package sv.asociacion.service;

import java.util.List;
import sv.asociacion.dao.MiembroCargoDAO;
import sv.asociacion.dao.PeriodoDirectivaDAO;
import sv.asociacion.domain.dto.PeriodoRequest;
import sv.asociacion.domain.dto.PeriodoResponse;
import sv.asociacion.domain.entity.PeriodoDirectiva;

public class PeriodoService {
    private final PeriodoDirectivaDAO periodoDAO;
    private final MiembroCargoDAO miembroCargoDAO;

    public PeriodoService(PeriodoDirectivaDAO periodoDAO) {
        this(periodoDAO, null);
    }

    public PeriodoService(PeriodoDirectivaDAO periodoDAO, MiembroCargoDAO miembroCargoDAO) {
        this.periodoDAO = periodoDAO;
        this.miembroCargoDAO = miembroCargoDAO;
    }

    public List<PeriodoResponse> findAll() {
        return periodoDAO.findFiltered(null, null).stream()
            .map(PeriodoResponse::from)
            .toList();
    }

    public List<PeriodoResponse> findFiltered(PeriodoDirectiva.Estado estado, String busqueda) {
        return periodoDAO.findFiltered(estado, busqueda).stream()
            .map(PeriodoResponse::from)
            .toList();
    }

    public PeriodoResponse findById(Integer id) {
        if (id == null) return null;
        return periodoDAO.findById(id).map(PeriodoResponse::from).orElse(null);
    }

    public PeriodoResponse findActivo() {
        return periodoDAO.findActivo().map(PeriodoResponse::from).orElse(null);
    }

    public PeriodoResponse create(PeriodoRequest request) {
        validar(request, null);

        PeriodoDirectiva entity = new PeriodoDirectiva();
        entity.setNombre(request.nombre().trim());
        entity.setFechaInicio(request.fechaInicio());
        entity.setFechaFin(request.fechaFin());
        entity.setEstado(request.estado() != null ? request.estado() : PeriodoDirectiva.Estado.PLANIFICADO);

        if (entity.getEstado() == PeriodoDirectiva.Estado.ACTIVO) {
            periodoDAO.finalizarActivosExcepto(null);
        }

        PeriodoDirectiva saved = periodoDAO.save(entity);
        return PeriodoResponse.from(saved);
    }

    public PeriodoResponse update(Integer id, PeriodoRequest request) {
        if (id == null) {
            throw new IllegalArgumentException("ID de período no especificado.");
        }
        PeriodoDirectiva existing = periodoDAO.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Período no encontrado."));

        if (existing.getEstado() == PeriodoDirectiva.Estado.FINALIZADO) {
            throw new IllegalStateException("No es posible editar un período directivo que ya ha finalizado.");
        }

        validar(request, id);

        existing.setNombre(request.nombre().trim());
        existing.setFechaInicio(request.fechaInicio());
        existing.setFechaFin(request.fechaFin());

        if (request.estado() != null) {
            if (request.estado() == PeriodoDirectiva.Estado.ACTIVO && existing.getEstado() != PeriodoDirectiva.Estado.ACTIVO) {
                periodoDAO.finalizarActivosExcepto(id);
            }
            existing.setEstado(request.estado());
        }

        periodoDAO.update(existing);
        return PeriodoResponse.from(existing);
    }

    public PeriodoResponse activar(Integer id) {
        if (id == null) {
            throw new IllegalArgumentException("ID de período requerido.");
        }
        PeriodoDirectiva existing = periodoDAO.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Período no encontrado."));

        if (existing.getEstado() == PeriodoDirectiva.Estado.FINALIZADO) {
            throw new IllegalStateException("No es posible activar un período directivo que ya ha finalizado.");
        }

        periodoDAO.finalizarActivosExcepto(id);
        periodoDAO.updateEstado(id, PeriodoDirectiva.Estado.ACTIVO);
        existing.setEstado(PeriodoDirectiva.Estado.ACTIVO);
        return PeriodoResponse.from(existing);
    }

    public PeriodoResponse finalizar(Integer id) {
        if (id == null) {
            throw new IllegalArgumentException("ID de período requerido.");
        }
        PeriodoDirectiva existing = periodoDAO.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Período no encontrado."));

        periodoDAO.updateEstado(id, PeriodoDirectiva.Estado.FINALIZADO);
        existing.setEstado(PeriodoDirectiva.Estado.FINALIZADO);
        return PeriodoResponse.from(existing);
    }

    public boolean delete(Integer id) {
        if (id == null) return false;
        PeriodoDirectiva existing = periodoDAO.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Período no encontrado."));

        if (existing.getEstado() != PeriodoDirectiva.Estado.PLANIFICADO) {
            throw new IllegalStateException("Solo se pueden eliminar períodos directivos en estado PLANIFICADO.");
        }

        if (miembroCargoDAO != null && !miembroCargoDAO.findByPeriodo(id).isEmpty()) {
            throw new IllegalStateException("No es posible eliminar el período porque contiene asignaciones directivas vinculadas.");
        }

        return periodoDAO.delete(id);
    }

    private void validar(PeriodoRequest req, Integer excludeId) {
        if (req == null) {
            throw new IllegalArgumentException("Los datos del período son obligatorios.");
        }
        if (req.nombre() == null || req.nombre().trim().isBlank()) {
            throw new IllegalArgumentException("El nombre del período es obligatorio (ej. 2024-2026).");
        }
        if (req.fechaInicio() == null || req.fechaFin() == null) {
            throw new IllegalArgumentException("Las fechas de inicio y finalización son obligatorias.");
        }
        if (req.fechaInicio().isAfter(req.fechaFin())) {
            throw new IllegalArgumentException("La fecha de inicio debe ser anterior o igual a la fecha de finalización.");
        }

        List<PeriodoDirectiva> overlapping = periodoDAO.findOverlapping(req.fechaInicio(), req.fechaFin(), excludeId);
        if (!overlapping.isEmpty()) {
            throw new IllegalStateException(
                "El rango de fechas (" + req.fechaInicio() + " a " + req.fechaFin() +
                ") se solapa con el período '" + overlapping.get(0).getNombre() + "'."
            );
        }
    }
}
