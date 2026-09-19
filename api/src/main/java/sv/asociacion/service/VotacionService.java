package sv.asociacion.service;

import java.time.LocalDateTime;
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

        // Solo se edita lo que no ha iniciado. Antes era una lista negra que bloqueaba
        // CERRADA y CANCELADA pero dejaba pasar ABIERTA: se podian cambiar titulo,
        // proyecto y fechas con la recepcion de votos en curso.
        if (existing.getEstado() != Votacion.Estado.BORRADOR
            && existing.getEstado() != Votacion.Estado.PROGRAMADA) {
            throw new IllegalStateException(
                "Solo es posible editar una votación no iniciada (estado actual: " + existing.getEstado() + ").");
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

        // Las fechas gobiernan la transicion. No hay planificador: es esta misma
        // operacion la que, segun el momento en que se invoque, deja la votacion
        // PROGRAMADA o la activa. Asi PROGRAMADA resulta alcanzable sin introducir
        // un endpoint ni una tarea periodica.
        LocalDateTime ahora = LocalDateTime.now();

        if (existing.getFechaFin() != null && !ahora.isBefore(existing.getFechaFin())) {
            throw new IllegalStateException(
                "No es posible abrir una votación cuya fecha de finalización ya transcurrió.");
        }

        Votacion.Estado destino =
            existing.getFechaInicio() != null && ahora.isBefore(existing.getFechaInicio())
                ? Votacion.Estado.PROGRAMADA
                : Votacion.Estado.ABIERTA;

        votacionDAO.updateEstado(id, destino);
        existing.setEstado(destino);
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

        // Cerrar es el acto que publica los resultados, de modo que solo puede cerrarse
        // lo que estuvo realmente abierto. Antes se aceptaba cerrar un BORRADOR o una
        // votacion CANCELADA, lo que habria publicado resultados de algo que nunca
        // recibio votos.
        if (existing.getEstado() != Votacion.Estado.ABIERTA) {
            throw new IllegalStateException(
                "Solo es posible cerrar una votación ABIERTA (estado actual: " + existing.getEstado() + ").");
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

    /**
     * Cuota de una opcion sobre el total, redondeada a un decimal como en
     * {@code ReunionService}. Sin votos publicados devuelve {@code 0.0} en lugar de
     * dividir entre cero.
     */
    private static double porcentaje(int votos, int total) {
        if (total <= 0) return 0.0;
        return Math.round((votos * 100.0 / total) * 10.0) / 10.0;
    }

    private VotacionResponse mapToResponse(Votacion v) {
        String nombreProyecto = null;
        if (v.getIdProyecto() != null && proyectoDAO != null) {
            nombreProyecto = proyectoDAO.findById(v.getIdProyecto()).map(Proyecto::getNombre).orElse(null);
        }

        // CERRADA es la publicacion de resultados. Antes de ese momento el servicio no
        // expone conteos: las opciones viajan con toda su informacion, pero con cero
        // votos. Una votacion CANCELADA tampoco publica. Asi se evita que un proceso en
        // curso muestre resultados parciales que influyan en quien aun no ha votado.
        boolean resultadosPublicados = v.getEstado() == Votacion.Estado.CERRADA;

        List<OpcionVotacion> ops = opcionDAO != null ? opcionDAO.findByVotacion(v.getIdVotacion()) : List.of();

        // El porcentaje es una cuota sobre el total, de modo que el total tiene que
        // conocerse antes de repartirlo: primero se cuenta, despues se calcula.
        int[] votosPorOpcion = new int[ops.size()];
        int totalVotos = 0;
        for (int i = 0; i < ops.size(); i++) {
            votosPorOpcion[i] = resultadosPublicados && votoDAO != null
                ? votoDAO.countByOpcion(ops.get(i).getIdOpcion())
                : 0;
            totalVotos += votosPorOpcion[i];
        }

        List<VotacionResponse.OpcionDetalle> opcionesDetalle = new ArrayList<>();
        for (int i = 0; i < ops.size(); i++) {
            OpcionVotacion op = ops.get(i);
            opcionesDetalle.add(new VotacionResponse.OpcionDetalle(
                op.getIdOpcion(), op.getDescripcion(), op.getOrden(),
                votosPorOpcion[i], porcentaje(votosPorOpcion[i], totalVotos)
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
