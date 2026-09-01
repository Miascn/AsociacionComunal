package sv.asociacion.service;

import java.time.LocalDate;
import java.util.List;
import sv.asociacion.dao.CargoDAO;
import sv.asociacion.dao.MiembroCargoDAO;
import sv.asociacion.dao.MiembroDAO;
import sv.asociacion.dao.PeriodoDirectivaDAO;
import sv.asociacion.domain.dto.AsignacionCargoRequest;
import sv.asociacion.domain.dto.AsignacionCargoResponse;
import sv.asociacion.domain.dto.RevocarAsignacionRequest;
import sv.asociacion.domain.entity.Cargo;
import sv.asociacion.domain.entity.Miembro;
import sv.asociacion.domain.entity.MiembroCargo;
import sv.asociacion.domain.entity.PeriodoDirectiva;

public class AsignacionCargoService {
    private final MiembroCargoDAO miembroCargoDAO;
    private final MiembroDAO miembroDAO;
    private final CargoDAO cargoDAO;
    private final PeriodoDirectivaDAO periodoDAO;

    public AsignacionCargoService(MiembroCargoDAO miembroCargoDAO, MiembroDAO miembroDAO,
                                  CargoDAO cargoDAO, PeriodoDirectivaDAO periodoDAO) {
        this.miembroCargoDAO = miembroCargoDAO;
        this.miembroDAO = miembroDAO;
        this.cargoDAO = cargoDAO;
        this.periodoDAO = periodoDAO;
    }

    public List<AsignacionCargoResponse> findDirectivaPeriodo(Integer idPeriodo) {
        return miembroCargoDAO.findDetalladoFiltered(idPeriodo, null, null);
    }

    public List<AsignacionCargoResponse> findDirectivaActual() {
        return periodoDAO.findActivo()
            .map(p -> miembroCargoDAO.findDetalladoFiltered(p.getIdPeriodo(), "ACTIVO", null))
            .orElse(List.of());
    }

    public List<AsignacionCargoResponse> findFiltered(Integer idPeriodo, String estado, String busqueda) {
        return miembroCargoDAO.findDetalladoFiltered(idPeriodo, estado, busqueda);
    }

    public AsignacionCargoResponse findById(Integer id) {
        if (id == null) return null;
        return miembroCargoDAO.findDetalladoById(id).orElse(null);
    }

    public AsignacionCargoResponse create(AsignacionCargoRequest req) {
        validarNuevaAsignacion(req, null);

        MiembroCargo entity = new MiembroCargo();
        entity.setIdMiembro(req.idMiembro());
        entity.setIdCargo(req.idCargo());
        entity.setIdPeriodo(req.idPeriodo());
        entity.setFechaAsignacion(req.fechaAsignacion());
        entity.setFechaFin(req.fechaFin());
        entity.setEstado(MiembroCargo.Estado.ACTIVO);

        MiembroCargo saved = miembroCargoDAO.save(entity);
        return miembroCargoDAO.findDetalladoById(saved.getIdMiembroCargo())
            .orElseGet(() -> mapFromRaw(saved));
    }

    public AsignacionCargoResponse revocar(Integer id, RevocarAsignacionRequest req) {
        if (id == null) {
            throw new IllegalArgumentException("ID de asignación no especificado.");
        }
        MiembroCargo existing = miembroCargoDAO.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Asignación de cargo no encontrada."));

        if (existing.getEstado() != MiembroCargo.Estado.ACTIVO) {
            throw new IllegalStateException("Solo se pueden revocar asignaciones de cargo en estado ACTIVO.");
        }

        LocalDate fechaFin = req != null && req.fechaFin() != null ? req.fechaFin() : LocalDate.now();
        if (fechaFin.isBefore(existing.getFechaAsignacion())) {
            throw new IllegalArgumentException("La fecha de revocación no puede ser anterior a la fecha de asignación.");
        }

        String motivo = req != null ? req.motivo() : null;
        miembroCargoDAO.revocar(id, fechaFin, motivo);

        return miembroCargoDAO.findDetalladoById(id).orElse(null);
    }

    public AsignacionCargoResponse finalizar(Integer id) {
        if (id == null) {
            throw new IllegalArgumentException("ID de asignación no especificado.");
        }
        MiembroCargo existing = miembroCargoDAO.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Asignación de cargo no encontrada."));

        existing.setEstado(MiembroCargo.Estado.FINALIZADO);
        if (existing.getFechaFin() == null) {
            existing.setFechaFin(LocalDate.now());
        }
        miembroCargoDAO.update(existing);

        return miembroCargoDAO.findDetalladoById(id).orElse(null);
    }

    public boolean delete(Integer id) {
        if (id == null) return false;
        MiembroCargo existing = miembroCargoDAO.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Asignación no encontrada."));

        PeriodoDirectiva p = periodoDAO.findById(existing.getIdPeriodo()).orElse(null);
        if (p != null && p.getEstado() == PeriodoDirectiva.Estado.FINALIZADO) {
            throw new IllegalStateException("No es posible eliminar asignaciones de un período directivo concluido.");
        }

        return miembroCargoDAO.delete(id);
    }

    private void validarNuevaAsignacion(AsignacionCargoRequest req, Integer excludeId) {
        if (req == null) {
            throw new IllegalArgumentException("Los datos de la asignación son requeridos.");
        }
        if (req.idMiembro() == null) {
            throw new IllegalArgumentException("Debe seleccionar un miembro.");
        }
        if (req.idCargo() == null) {
            throw new IllegalArgumentException("Debe seleccionar un cargo directivo.");
        }
        if (req.idPeriodo() == null) {
            throw new IllegalArgumentException("Debe seleccionar un período directivo.");
        }
        if (req.fechaAsignacion() == null) {
            throw new IllegalArgumentException("La fecha de asignación es obligatoria.");
        }

        Miembro miembro = miembroDAO.findById(req.idMiembro())
            .orElseThrow(() -> new IllegalArgumentException("El miembro seleccionado no existe."));
        if (miembro.getEstado() != Miembro.Estado.ACTIVO) {
            throw new IllegalStateException("Solo los miembros en estado ACTIVO pueden asumir cargos en la junta directiva.");
        }

        Cargo cargo = cargoDAO.findById(req.idCargo())
            .orElseThrow(() -> new IllegalArgumentException("El cargo seleccionado no existe."));
        if (!cargo.isActivo()) {
            throw new IllegalStateException("El cargo '" + cargo.getNombre() + "' se encuentra inactivo y no puede ser asignado.");
        }

        PeriodoDirectiva periodo = periodoDAO.findById(req.idPeriodo())
            .orElseThrow(() -> new IllegalArgumentException("El período seleccionado no existe."));
        if (periodo.getEstado() == PeriodoDirectiva.Estado.FINALIZADO) {
            throw new IllegalStateException("No se pueden asignar cargos a un período directivo que ya ha finalizado.");
        }

        // Validación de fechas dentro del período
        if (periodo.getFechaInicio() != null && req.fechaAsignacion().isBefore(periodo.getFechaInicio())) {
            throw new IllegalArgumentException(
                "La fecha de asignación (" + req.fechaAsignacion() +
                ") no puede ser anterior al inicio del período (" + periodo.getFechaInicio() + ")."
            );
        }
        if (periodo.getFechaFin() != null && req.fechaAsignacion().isAfter(periodo.getFechaFin())) {
            throw new IllegalArgumentException(
                "La fecha de asignación (" + req.fechaAsignacion() +
                ") no puede ser posterior a la finalización del período (" + periodo.getFechaFin() + ")."
            );
        }
        if (req.fechaFin() != null) {
            if (req.fechaFin().isBefore(req.fechaAsignacion())) {
                throw new IllegalArgumentException("La fecha de fin no puede ser anterior a la de asignación.");
            }
            if (periodo.getFechaFin() != null && req.fechaFin().isAfter(periodo.getFechaFin())) {
                throw new IllegalArgumentException("La fecha de fin excede la vigencia del período directivo.");
            }
        }

        // Regla: No duplicidad de cargo en el período
        if (miembroCargoDAO.findActiveByCargoAndPeriodo(req.idCargo(), req.idPeriodo(), excludeId).isPresent()) {
            throw new IllegalStateException("El cargo '" + cargo.getNombre() + "' ya está ocupado activamente en este período directivo.");
        }

        // Regla: No doble cargo simultáneo para el mismo miembro en el mismo período
        if (miembroCargoDAO.findActiveByMiembroAndPeriodo(req.idMiembro(), req.idPeriodo(), excludeId).isPresent()) {
            throw new IllegalStateException(
                "El miembro " + miembro.getNombre() + " " + miembro.getApellido() +
                " ya cuenta con un cargo activo en este período directivo."
            );
        }
    }

    private AsignacionCargoResponse mapFromRaw(MiembroCargo mc) {
        Miembro m = miembroDAO.findById(mc.getIdMiembro()).orElse(null);
        Cargo c = cargoDAO.findById(mc.getIdCargo()).orElse(null);
        PeriodoDirectiva p = periodoDAO.findById(mc.getIdPeriodo()).orElse(null);

        return new AsignacionCargoResponse(
            mc.getIdMiembroCargo(),
            mc.getIdMiembro(),
            m != null ? m.getNombre() + " " + m.getApellido() : "",
            m != null ? m.getDui() : "",
            m != null ? m.getTelefono() : "",
            mc.getIdCargo(),
            c != null ? c.getNombre() : "",
            c != null ? c.getNivelJerarquico() : 1,
            mc.getIdPeriodo(),
            p != null ? p.getNombre() : "",
            p != null && p.getEstado() != null ? p.getEstado().name() : "",
            mc.getFechaAsignacion() != null ? mc.getFechaAsignacion().toString() : "",
            mc.getFechaFin() != null ? mc.getFechaFin().toString() : null,
            mc.getMotivoSalida(),
            mc.getEstado() != null ? mc.getEstado().name() : "ACTIVO"
        );
    }
}
