package sv.asociacion.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import sv.asociacion.dao.AportacionDAO;
import sv.asociacion.dao.ProyectoDAO;
import sv.asociacion.dao.UsuarioDAO;
import sv.asociacion.dao.VotacionDAO;
import sv.asociacion.domain.dto.ProyectoRequest;
import sv.asociacion.domain.dto.ProyectoResponse;
import sv.asociacion.domain.entity.Proyecto;

public class ProyectoService {
    private final ProyectoDAO proyectoDAO;
    private final UsuarioDAO usuarioDAO;
    private final AportacionDAO aportacionDAO;
    private final VotacionDAO votacionDAO;

    public ProyectoService(ProyectoDAO proyectoDAO, UsuarioDAO usuarioDAO,
                           AportacionDAO aportacionDAO, VotacionDAO votacionDAO) {
        this.proyectoDAO = proyectoDAO;
        this.usuarioDAO = usuarioDAO;
        this.aportacionDAO = aportacionDAO;
        this.votacionDAO = votacionDAO;
    }

    public List<ProyectoResponse> findAll() {
        return proyectoDAO.findFiltered(null, null, null).stream()
            .map(d -> buildResponse(d.proyecto(), d.nombreCreador()))
            .toList();
    }

    public List<ProyectoResponse> findFiltered(Proyecto.Estado estado, Integer creadoPor, String busqueda) {
        return proyectoDAO.findFiltered(estado, creadoPor, busqueda).stream()
            .map(d -> buildResponse(d.proyecto(), d.nombreCreador()))
            .toList();
    }

    public ProyectoResponse findById(Integer id) {
        if (id == null) return null;
        return proyectoDAO.findByIdWithCreator(id)
            .map(d -> buildResponse(d.proyecto(), d.nombreCreador()))
            .orElse(null);
    }

    public ProyectoResponse create(ProyectoRequest request) {
        validar(request, null);

        Proyecto entity = new Proyecto();
        entity.setCreadoPor(request.creadoPor() != null ? request.creadoPor() : 1);
        entity.setNombre(request.nombre().trim());
        entity.setDescripcion(request.descripcion() != null ? request.descripcion().trim() : "");
        entity.setPresupuesto(request.presupuesto() != null ? request.presupuesto() : BigDecimal.ZERO);
        entity.setFechaCreacion(LocalDate.now());
        entity.setEstado(request.estado() != null ? request.estado() : Proyecto.Estado.BORRADOR);

        Proyecto saved = proyectoDAO.save(entity);
        return findById(saved.getIdProyecto());
    }

    public ProyectoResponse update(Integer id, ProyectoRequest request) {
        if (id == null) {
            throw new IllegalArgumentException("ID de proyecto no especificado.");
        }
        Proyecto existing = proyectoDAO.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Proyecto no encontrado."));

        if (existing.getEstado() == Proyecto.Estado.FINALIZADO) {
            throw new IllegalStateException("No es posible editar un proyecto finalizado.");
        }

        validar(request, id);

        existing.setNombre(request.nombre().trim());
        existing.setDescripcion(request.descripcion() != null ? request.descripcion().trim() : "");
        existing.setPresupuesto(request.presupuesto() != null ? request.presupuesto() : BigDecimal.ZERO);
        if (request.creadoPor() != null) {
            existing.setCreadoPor(request.creadoPor());
        }

        proyectoDAO.update(existing);
        return findById(id);
    }

    public ProyectoResponse cambiarEstado(Integer id, Proyecto.Estado nuevoEstado) {
        if (id == null || nuevoEstado == null) {
            throw new IllegalArgumentException("ID y nuevo estado son requeridos.");
        }
        Proyecto existing = proyectoDAO.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Proyecto no encontrado."));

        validarTransicion(existing.getEstado(), nuevoEstado);

        proyectoDAO.updateEstado(id, nuevoEstado);
        return findById(id);
    }

    public boolean delete(Integer id) {
        if (id == null) return false;
        Proyecto existing = proyectoDAO.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Proyecto no encontrado."));

        if (existing.getEstado() != Proyecto.Estado.BORRADOR && existing.getEstado() != Proyecto.Estado.RECHAZADO) {
            throw new IllegalStateException("Solo se pueden eliminar proyectos en estado BORRADOR o RECHAZADO.");
        }

        // El historial de decisiones comunitarias no se borra: un proyecto sometido a
        // votacion conserva su vinculo aunque despues se rechace. La base de datos
        // respalda esta misma regla con fk_votacion_proyecto ON DELETE RESTRICT.
        if (votacionDAO != null && !votacionDAO.findByProyecto(id).isEmpty()) {
            throw new IllegalStateException("No es posible eliminar el proyecto porque tiene votaciones asociadas.");
        }

        if (aportacionDAO != null && aportacionDAO.countFiltered(null, id, null, null, null, null, null, null) > 0) {
            throw new IllegalStateException("No es posible eliminar el proyecto porque tiene aportaciones vinculadas.");
        }

        return proyectoDAO.delete(id);
    }

    public void validarTransicion(Proyecto.Estado actual, Proyecto.Estado nuevo) {
        if (actual == nuevo) return;

        boolean valida = switch (actual) {
            case BORRADOR -> nuevo == Proyecto.Estado.PROPUESTO;
            case PROPUESTO -> nuevo == Proyecto.Estado.APROBADO
                           || nuevo == Proyecto.Estado.RECHAZADO
                           || nuevo == Proyecto.Estado.BORRADOR;
            case APROBADO -> nuevo == Proyecto.Estado.EN_EJECUCION
                          || nuevo == Proyecto.Estado.RECHAZADO;
            case EN_EJECUCION -> nuevo == Proyecto.Estado.FINALIZADO;
            case RECHAZADO -> nuevo == Proyecto.Estado.BORRADOR;
            case FINALIZADO -> false;
        };

        if (!valida) {
            throw new IllegalStateException(
                "Transición no permitida: no se puede avanzar de " + actual + " a " + nuevo + "."
            );
        }
    }

    private void validar(ProyectoRequest req, Integer excludeId) {
        if (req == null) {
            throw new IllegalArgumentException("Los datos del proyecto son obligatorios.");
        }
        if (req.nombre() == null || req.nombre().trim().isBlank()) {
            throw new IllegalArgumentException("El nombre del proyecto es obligatorio.");
        }
        if (req.presupuesto() != null && req.presupuesto().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El presupuesto del proyecto no puede ser negativo.");
        }
        if (req.creadoPor() != null && usuarioDAO != null) {
            if (usuarioDAO.findById(req.creadoPor()).isEmpty()) {
                throw new IllegalArgumentException("El usuario creador seleccionado no existe.");
            }
        }

        proyectoDAO.findByNombre(req.nombre().trim()).ifPresent(p -> {
            if (excludeId == null || !p.getIdProyecto().equals(excludeId)) {
                throw new IllegalStateException("Ya existe un proyecto con el nombre '" + req.nombre().trim() + "'.");
            }
        });
    }

    private ProyectoResponse buildResponse(Proyecto proyecto, String nombreCreador) {
        BigDecimal montoRecaudado = BigDecimal.ZERO;
        int totalAportantes = 0;
        if (aportacionDAO != null && proyecto.getIdProyecto() != null) {
            try {
                montoRecaudado = aportacionDAO.sumFiltered(null, proyecto.getIdProyecto(), null, null, null, null, "REGISTRADA", null);
                totalAportantes = aportacionDAO.countFiltered(null, proyecto.getIdProyecto(), null, null, null, null, "REGISTRADA", null);
            } catch (Exception ignored) {}
        }

        Integer idVotacion = null;
        String tituloVotacion = null;
        if (votacionDAO != null && proyecto.getIdProyecto() != null) {
            try {
                var votaciones = votacionDAO.findByProyecto(proyecto.getIdProyecto());
                if (!votaciones.isEmpty()) {
                    idVotacion = votaciones.get(0).getIdVotacion();
                    tituloVotacion = votaciones.get(0).getTitulo();
                }
            } catch (Exception ignored) {}
        }

        BigDecimal aportePorMiembro = BigDecimal.ZERO;
        if (proyecto.getPresupuesto() != null && proyecto.getPresupuesto().compareTo(BigDecimal.ZERO) > 0) {
            int baseMembers = 50;
            aportePorMiembro = proyecto.getPresupuesto().divide(BigDecimal.valueOf(baseMembers), 2, java.math.RoundingMode.HALF_UP);
        }

        return ProyectoResponse.from(
            proyecto,
            nombreCreador,
            montoRecaudado,
            idVotacion,
            tituloVotacion,
            totalAportantes,
            aportePorMiembro
        );
    }
}