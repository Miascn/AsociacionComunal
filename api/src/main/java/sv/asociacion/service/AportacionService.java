package sv.asociacion.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import sv.asociacion.dao.AportacionDAO;
import sv.asociacion.dao.MiembroDAO;
import sv.asociacion.dao.ProyectoDAO;
import sv.asociacion.domain.dto.AportacionPageResponse;
import sv.asociacion.domain.dto.AportacionRequest;
import sv.asociacion.domain.dto.AportacionResponse;
import sv.asociacion.domain.entity.Aportacion;

public class AportacionService {
    private final AportacionDAO aportacionDAO;
    private final MiembroDAO miembroDAO;
    private final ProyectoDAO proyectoDAO;

    public AportacionService(AportacionDAO aportacionDAO, MiembroDAO miembroDAO, ProyectoDAO proyectoDAO) {
        this.aportacionDAO = aportacionDAO;
        this.miembroDAO = miembroDAO;
        this.proyectoDAO = proyectoDAO;
    }

    public AportacionResponse create(AportacionRequest request) {
        validar(request, null);

        Aportacion entity = new Aportacion();
        entity.setIdMiembro(request.idMiembro());
        entity.setIdProyecto(request.idProyecto());
        entity.setPeriodoMes(request.periodoMes().trim());
        entity.setMonto(request.monto());
        entity.setFechaPago(request.fechaPago());
        entity.setMetodoPago(request.metodoPago());
        entity.setReferencia(request.referencia() != null ? request.referencia().trim() : null);
        entity.setEstado(Aportacion.Estado.REGISTRADA);

        Aportacion saved = aportacionDAO.save(entity);
        return findById(saved.getIdAportacion());
    }

    public AportacionResponse update(Long id, AportacionRequest request) {
        if (id == null) {
            throw new IllegalArgumentException("ID de aportación no especificado.");
        }
        Aportacion existing = aportacionDAO.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Aportación no encontrada."));

        if (existing.getEstado() == Aportacion.Estado.ANULADA) {
            throw new IllegalStateException("No se puede editar una aportación que ha sido anulada.");
        }

        validar(request, id);

        existing.setIdMiembro(request.idMiembro());
        existing.setIdProyecto(request.idProyecto());
        existing.setPeriodoMes(request.periodoMes().trim());
        existing.setMonto(request.monto());
        existing.setFechaPago(request.fechaPago());
        existing.setMetodoPago(request.metodoPago());
        existing.setReferencia(request.referencia() != null ? request.referencia().trim() : null);

        aportacionDAO.update(existing);
        return findById(id);
    }

    public boolean anular(Long id) {
        if (id == null) return false;
        Aportacion existing = aportacionDAO.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Aportación no encontrada."));
        if (existing.getEstado() == Aportacion.Estado.ANULADA) {
            return true;
        }
        return aportacionDAO.anular(id);
    }

    public AportacionResponse findById(Long id) {
        if (id == null) return null;
        return aportacionDAO.findByIdWithDetails(id)
            .map(d -> AportacionResponse.from(d.aportacion(), d.nombreMiembro(), d.duiMiembro(), d.nombreProyecto()))
            .orElse(null);
    }

    public AportacionPageResponse findPage(Integer idMiembro, Integer idProyecto, String periodo,
                                          String desde, String hasta, String metodo, String estado,
                                          String busqueda, int page, int size) {
        int safePage = Math.max(1, page);
        int safeSize = size <= 0 ? 20 : Math.min(size, 100);
        int offset = (safePage - 1) * safeSize;

        int total = aportacionDAO.countFiltered(idMiembro, idProyecto, periodo, desde, hasta, metodo, estado, busqueda);
        BigDecimal totalRecaudado = aportacionDAO.sumFiltered(idMiembro, idProyecto, periodo, desde, hasta, metodo, estado, busqueda);
        List<AportacionDAO.AportacionDetail> details = aportacionDAO.findFiltered(
            idMiembro, idProyecto, periodo, desde, hasta, metodo, estado, busqueda, offset, safeSize
        );

        List<AportacionResponse> items = details.stream()
            .map(d -> AportacionResponse.from(d.aportacion(), d.nombreMiembro(), d.duiMiembro(), d.nombreProyecto()))
            .toList();

        return AportacionPageResponse.of(items, total, totalRecaudado, safePage, safeSize);
    }

    private void validar(AportacionRequest req, Long excludeId) {
        if (req == null) {
            throw new IllegalArgumentException("Los datos de la aportación son obligatorios.");
        }
        if (req.idMiembro() == null) {
            throw new IllegalArgumentException("El miembro asociado es obligatorio.");
        }
        if (miembroDAO != null && miembroDAO.findById(req.idMiembro()).isEmpty()) {
            throw new IllegalArgumentException("El miembro seleccionado no existe en el sistema.");
        }
        if (req.idProyecto() != null && proyectoDAO != null) {
            if (proyectoDAO.findById(req.idProyecto()).isEmpty()) {
                throw new IllegalArgumentException("El proyecto seleccionado no existe.");
            }
        }
        if (req.monto() == null || req.monto().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto debe ser un valor monetario positivo mayor a cero.");
        }
        if (req.fechaPago() == null) {
            throw new IllegalArgumentException("La fecha de pago es obligatoria.");
        }
        if (req.fechaPago().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("La fecha de pago no puede ser posterior a la fecha actual.");
        }
        if (req.periodoMes() == null || !req.periodoMes().trim().matches("^\\d{4}-(0[1-9]|1[0-2])$")) {
            throw new IllegalArgumentException("El período debe tener formato de mes válido: YYYY-MM (ej. 2026-03).");
        }
        if (req.metodoPago() == null) {
            throw new IllegalArgumentException("El método de pago es obligatorio.");
        }
        if (aportacionDAO.existsByMiembroAndPeriodo(req.idMiembro(), req.idProyecto(), req.periodoMes().trim(), excludeId)) {
            throw new IllegalStateException("Ya existe una aportación registrada para este miembro en el período " + req.periodoMes().trim() + ".");
        }
    }
}
