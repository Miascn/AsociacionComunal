package sv.asociacion.service;

import java.util.ArrayList;
import java.util.List;
import sv.asociacion.dao.OpcionVotacionDAO;
import sv.asociacion.dao.ProyectoDAO;
import sv.asociacion.dao.VotacionDAO;
import sv.asociacion.dao.VotoDAO;
import sv.asociacion.domain.dto.VotacionRequest;
import sv.asociacion.domain.dto.VotacionResponse;
import sv.asociacion.domain.entity.OpcionVotacion;
import sv.asociacion.domain.entity.Proyecto;
import sv.asociacion.domain.entity.Votacion;

public class VotacionService {
    private final VotacionDAO votacionDAO;
    private final OpcionVotacionDAO opcionDAO;
    private final VotoDAO votoDAO;
    private final ProyectoDAO proyectoDAO;

    public VotacionService(VotacionDAO votacionDAO, OpcionVotacionDAO opcionDAO,
                           VotoDAO votoDAO, ProyectoDAO proyectoDAO) {
        this.votacionDAO = votacionDAO;
        this.opcionDAO = opcionDAO;
        this.votoDAO = votoDAO;
        this.proyectoDAO = proyectoDAO;
    }

    public List<VotacionResponse> findAll() {
        return findFiltered(null, null, null);
    }

    public List<VotacionResponse> findFiltered(Votacion.Estado estado, Integer idProyecto, String busqueda) {
        return votacionDAO.findFiltered(estado, idProyecto, busqueda).stream()
            .map(this::mapToResponse)
            .toList();
    }

    public VotacionResponse findById(Integer id) {
        if (id == null) return null;
        return votacionDAO.findById(id).map(this::mapToResponse).orElse(null);
    }

    public VotacionResponse create(VotacionRequest req) {
        validarDatos(req);

        Votacion entity = new Votacion();
        entity.setTitulo(req.titulo().trim());
        entity.setDescripcion(req.descripcion() != null ? req.descripcion().trim() : null);
        entity.setIdProyecto(req.idProyecto());
        entity.setFechaInicio(req.fechaInicio());
        entity.setFechaFin(req.fechaFin());
        entity.setEstado(Votacion.Estado.BORRADOR);

        Votacion saved = votacionDAO.save(entity);

        // Pre-registro de opciones si vienen incluidas
        if (req.opcionesIniciales() != null && !req.opcionesIniciales().isEmpty()) {
            short orden = 1;
            for (String textoOpcion : req.opcionesIniciales()) {
                if (textoOpcion != null && !textoOpcion.trim().isBlank()) {
                    OpcionVotacion opcion = new OpcionVotacion();
                    opcion.setIdVotacion(saved.getIdVotacion());
                    opcion.setDescripcion(textoOpcion.trim());
                    opcion.setOrden(orden++);
                    opcionDAO.save(opcion);
                }
            }
        }

        return mapToResponse(saved);
    }

    public VotacionResponse update(Integer id, VotacionRequest req) {
        if (id == null) {
            throw new IllegalArgumentException("ID de votación requerido.");
        }
        Votacion existing = votacionDAO.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Votación no encontrada."));

        if (existing.getEstado() == Votacion.Estado.CERRADA || existing.getEstado() == Votacion.Estado.CANCELADA) {
            throw new IllegalStateException("No es posible modificar una votación en estado " + existing.getEstado() + ".");
        }

        validarDatos(req);

        existing.setTitulo(req.titulo().trim());
        existing.setDescripcion(req.descripcion() != null ? req.descripcion().trim() : null);
        existing.setIdProyecto(req.idProyecto());
        existing.setFechaInicio(req.fechaInicio());
        existing.setFechaFin(req.fechaFin());

        votacionDAO.update(existing);
        return mapToResponse(existing);
    }

    public VotacionResponse abrir(Integer id) {
        if (id == null) {
            throw new IllegalArgumentException("ID de votación requerido.");
        }
        Votacion existing = votacionDAO.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Votación no encontrada."));

        if (existing.getEstado() == Votacion.Estado.ABIERTA) {
            return mapToResponse(existing);
        }
        if (existing.getEstado() == Votacion.Estado.CERRADA) {
            throw new IllegalStateException("No es posible reabrir una votación que ya ha sido cerrada.");
        }
        if (existing.getEstado() == Votacion.Estado.CANCELADA) {
            throw new IllegalStateException("No es posible abrir una votación cancelada.");
        }

        List<OpcionVotacion> opciones = opcionDAO.findByVotacion(id);
        if (opciones.size() < 2) {
            throw new IllegalStateException("Para abrir una votación se requieren al menos dos opciones registradas.");
        }

        votacionDAO.updateEstado(id, Votacion.Estado.ABIERTA);
        existing.setEstado(Votacion.Estado.ABIERTA);
        return mapToResponse(existing);
    }

    public VotacionResponse cerrar(Integer id) {
        if (id == null) {
            throw new IllegalArgumentException("ID de votación requerido.");
        }
        Votacion existing = votacionDAO.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Votación no encontrada."));

        if (existing.getEstado() == Votacion.Estado.CERRADA) {
            return mapToResponse(existing);
        }

        votacionDAO.updateEstado(id, Votacion.Estado.CERRADA);
        existing.setEstado(Votacion.Estado.CERRADA);
        return mapToResponse(existing);
    }

    public VotacionResponse cancelar(Integer id) {
        if (id == null) {
            throw new IllegalArgumentException("ID de votación requerido.");
        }
        Votacion existing = votacionDAO.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Votación no encontrada."));

        if (existing.getEstado() == Votacion.Estado.CERRADA) {
            throw new IllegalStateException("Una votación cerrada no puede ser cancelada.");
        }

        votacionDAO.updateEstado(id, Votacion.Estado.CANCELADA);
        existing.setEstado(Votacion.Estado.CANCELADA);
        return mapToResponse(existing);
    }

    public boolean delete(Integer id) {
        if (id == null) return false;
        Votacion existing = votacionDAO.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Votación no encontrada."));

        if (existing.getEstado() == Votacion.Estado.ABIERTA || existing.getEstado() == Votacion.Estado.CERRADA) {
            throw new IllegalStateException("Solo se pueden eliminar votaciones en estado BORRADOR o CANCELADA.");
        }

        int votos = votoDAO.countByVotacion(id);
        if (votos > 0) {
            throw new IllegalStateException("No es posible eliminar una votación con votos emitidos.");
        }

        // Eliminar opciones hijas primero si existen
        List<OpcionVotacion> opciones = opcionDAO.findByVotacion(id);
        for (OpcionVotacion op : opciones) {
            opcionDAO.delete(op.getIdOpcion());
        }

        return votacionDAO.delete(id);
    }

    private void validarDatos(VotacionRequest req) {
        if (req == null) {
            throw new IllegalArgumentException("Los datos de la votación son requeridos.");
        }
        if (req.titulo() == null || req.titulo().trim().isBlank()) {
            throw new IllegalArgumentException("El título de la votación es obligatorio.");
        }
        if (req.fechaInicio() == null || req.fechaFin() == null) {
            throw new IllegalArgumentException("Las fechas de inicio y fin de votación son obligatorias.");
        }
        if (!req.fechaInicio().isBefore(req.fechaFin())) {
            throw new IllegalArgumentException("La fecha de inicio debe ser anterior a la fecha de finalización.");
        }
        if (req.idProyecto() != null && proyectoDAO != null) {
            Proyecto p = proyectoDAO.findById(req.idProyecto()).orElse(null);
            if (p == null) {
                throw new IllegalArgumentException("El proyecto asociado no existe.");
            }
        }
    }

    private VotacionResponse mapToResponse(Votacion v) {
        String nombreProyecto = null;
        if (v.getIdProyecto() != null && proyectoDAO != null) {
            nombreProyecto = proyectoDAO.findById(v.getIdProyecto()).map(Proyecto::getNombre).orElse(null);
        }

        List<OpcionVotacion> ops = opcionDAO != null ? opcionDAO.findByVotacion(v.getIdVotacion()) : List.of();
        List<VotacionResponse.OpcionDetalle> opcionesDetalle = new ArrayList<>();
        int totalVotos = 0;

        for (OpcionVotacion op : ops) {
            int votosOp = votoDAO != null ? votoDAO.countByOpcion(op.getIdOpcion()) : 0;
            totalVotos += votosOp;
            opcionesDetalle.add(new VotacionResponse.OpcionDetalle(
                op.getIdOpcion(), op.getDescripcion(), op.getOrden(), votosOp
            ));
        }

        return new VotacionResponse(
            v.getIdVotacion(),
            v.getTitulo(),
            v.getDescripcion(),
            v.getIdProyecto(),
            nombreProyecto,
            v.getFechaInicio() != null ? v.getFechaInicio().toString() : "",
            v.getFechaFin() != null ? v.getFechaFin().toString() : "",
            v.getEstado() != null ? v.getEstado().name() : "BORRADOR",
            ops.size(),
            totalVotos,
            opcionesDetalle
        );
    }
}
