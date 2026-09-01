package sv.asociacion.service;

import java.util.List;
import sv.asociacion.dao.OpcionVotacionDAO;
import sv.asociacion.dao.VotacionDAO;
import sv.asociacion.dao.VotoDAO;
import sv.asociacion.domain.dto.OpcionVotacionRequest;
import sv.asociacion.domain.dto.OpcionVotacionResponse;
import sv.asociacion.domain.entity.OpcionVotacion;
import sv.asociacion.domain.entity.Votacion;

public class OpcionVotacionService {
    private final OpcionVotacionDAO opcionDAO;
    private final VotacionDAO votacionDAO;
    private final VotoDAO votoDAO;

    public OpcionVotacionService(OpcionVotacionDAO opcionDAO, VotacionDAO votacionDAO, VotoDAO votoDAO) {
        this.opcionDAO = opcionDAO;
        this.votacionDAO = votacionDAO;
        this.votoDAO = votoDAO;
    }

    public List<OpcionVotacionResponse> findByVotacion(Integer idVotacion) {
        if (idVotacion == null) {
            throw new IllegalArgumentException("ID de votación requerido.");
        }
        Votacion v = votacionDAO.findById(idVotacion)
            .orElseThrow(() -> new IllegalArgumentException("Votación no encontrada con ID: " + idVotacion));

        return opcionDAO.findByVotacion(idVotacion).stream()
            .map(op -> mapToResponse(op, v))
            .toList();
    }

    public OpcionVotacionResponse findById(Integer id) {
        if (id == null) return null;
        OpcionVotacion op = opcionDAO.findById(id).orElse(null);
        if (op == null) return null;
        Votacion v = votacionDAO.findById(op.getIdVotacion()).orElse(null);
        return mapToResponse(op, v);
    }

    public OpcionVotacionResponse create(OpcionVotacionRequest req) {
        if (req == null) {
            throw new IllegalArgumentException("Los datos de la opción son requeridos.");
        }
        if (req.idVotacion() == null) {
            throw new IllegalArgumentException("El ID de votación es obligatorio.");
        }
        if (req.descripcion() == null || req.descripcion().trim().isBlank()) {
            throw new IllegalArgumentException("La descripción de la opción es obligatoria.");
        }
        if (req.descripcion().trim().length() > 120) {
            throw new IllegalArgumentException("La descripción no puede exceder 120 caracteres.");
        }

        Votacion v = votacionDAO.findById(req.idVotacion())
            .orElseThrow(() -> new IllegalArgumentException("Votación no encontrada con ID: " + req.idVotacion()));

        validarVotacionModificable(v);

        // Validar que no exista otra opción idéntica en la misma votación
        List<OpcionVotacion> existentes = opcionDAO.findByVotacion(req.idVotacion());
        String nuevaDesc = req.descripcion().trim();
        for (OpcionVotacion e : existentes) {
            if (e.getDescripcion().equalsIgnoreCase(nuevaDesc)) {
                throw new IllegalArgumentException("Ya existe una opción con la misma descripción en esta votación.");
            }
        }

        short orden;
        if (req.orden() != null && req.orden() > 0) {
            orden = req.orden();
        } else {
            orden = (short) (opcionDAO.findMaxOrdenByVotacion(req.idVotacion()) + 1);
        }

        OpcionVotacion entity = new OpcionVotacion();
        entity.setIdVotacion(req.idVotacion());
        entity.setDescripcion(nuevaDesc);
        entity.setOrden(orden);

        OpcionVotacion saved = opcionDAO.save(entity);
        return mapToResponse(saved, v);
    }

    public OpcionVotacionResponse update(Integer id, OpcionVotacionRequest req) {
        if (id == null) {
            throw new IllegalArgumentException("ID de opción requerido.");
        }
        if (req == null || req.descripcion() == null || req.descripcion().trim().isBlank()) {
            throw new IllegalArgumentException("La descripción de la opción es obligatoria.");
        }
        if (req.descripcion().trim().length() > 120) {
            throw new IllegalArgumentException("La descripción no puede exceder 120 caracteres.");
        }

        OpcionVotacion existing = opcionDAO.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Opción no encontrada con ID: " + id));

        Votacion v = votacionDAO.findById(existing.getIdVotacion())
            .orElseThrow(() -> new IllegalArgumentException("Votación no encontrada."));

        validarVotacionModificable(v);

        String nuevaDesc = req.descripcion().trim();
        List<OpcionVotacion> existentes = opcionDAO.findByVotacion(existing.getIdVotacion());
        for (OpcionVotacion e : existentes) {
            if (!e.getIdOpcion().equals(id) && e.getDescripcion().equalsIgnoreCase(nuevaDesc)) {
                throw new IllegalArgumentException("Ya existe otra opción con la misma descripción en esta votación.");
            }
        }

        existing.setDescripcion(nuevaDesc);
        if (req.orden() != null && req.orden() > 0) {
            existing.setOrden(req.orden());
        }

        opcionDAO.update(existing);
        return mapToResponse(existing, v);
    }

    public void reordenar(Integer idVotacion, List<Integer> idsEnOrden) {
        if (idVotacion == null) {
            throw new IllegalArgumentException("ID de votación requerido.");
        }
        if (idsEnOrden == null || idsEnOrden.isEmpty()) {
            return;
        }

        Votacion v = votacionDAO.findById(idVotacion)
            .orElseThrow(() -> new IllegalArgumentException("Votación no encontrada."));

        validarVotacionModificable(v);

        short nuevoOrden = 1;
        for (Integer idOpcion : idsEnOrden) {
            OpcionVotacion op = opcionDAO.findById(idOpcion).orElse(null);
            if (op != null && op.getIdVotacion().equals(idVotacion)) {
                op.setOrden(nuevoOrden++);
                opcionDAO.update(op);
            }
        }
    }

    public boolean delete(Integer id) {
        if (id == null) return false;

        OpcionVotacion existing = opcionDAO.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Opción no encontrada con ID: " + id));

        Votacion v = votacionDAO.findById(existing.getIdVotacion())
            .orElseThrow(() -> new IllegalArgumentException("Votación no encontrada."));

        validarVotacionModificable(v);

        if (votoDAO != null && votoDAO.countByOpcion(id) > 0) {
            throw new IllegalStateException("No es posible eliminar una opción que ya posee votos registrados.");
        }

        return opcionDAO.delete(id);
    }

    private void validarVotacionModificable(Votacion v) {
        if (v.getEstado() != Votacion.Estado.BORRADOR && v.getEstado() != Votacion.Estado.PROGRAMADA) {
            throw new IllegalStateException("No es posible editar o eliminar opciones después de que la votación ha sido abierta.");
        }
    }

    private OpcionVotacionResponse mapToResponse(OpcionVotacion op, Votacion v) {
        String titulo = v != null ? v.getTitulo() : "";
        String estado = v != null && v.getEstado() != null ? v.getEstado().name() : "BORRADOR";
        boolean editable = v != null && (v.getEstado() == Votacion.Estado.BORRADOR || v.getEstado() == Votacion.Estado.PROGRAMADA);
        int votos = votoDAO != null && op.getIdOpcion() != null ? votoDAO.countByOpcion(op.getIdOpcion()) : 0;

        return new OpcionVotacionResponse(
            op.getIdOpcion(),
            op.getIdVotacion(),
            titulo,
            estado,
            op.getDescripcion(),
            op.getOrden() != null ? op.getOrden() : 1,
            votos,
            editable
        );
    }
}
