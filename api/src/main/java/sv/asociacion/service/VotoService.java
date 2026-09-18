package sv.asociacion.service;

import java.time.LocalDateTime;
import java.util.Optional;
import sv.asociacion.dao.MiembroDAO;
import sv.asociacion.dao.OpcionVotacionDAO;
import sv.asociacion.dao.VotacionDAO;
import sv.asociacion.dao.VotoDAO;
import sv.asociacion.dao.VotoDuplicadoException;
import sv.asociacion.domain.dto.EmitirVotoRequest;
import sv.asociacion.domain.dto.EmitirVotoResponse;
import sv.asociacion.domain.dto.EstadoParticipacionResponse;
import sv.asociacion.domain.entity.Miembro;
import sv.asociacion.domain.entity.OpcionVotacion;
import sv.asociacion.domain.entity.Votacion;
import sv.asociacion.domain.entity.Voto;
import sv.asociacion.util.DateUtils;

public class VotoService {
    private final VotoDAO votoDAO;
    private final VotacionDAO votacionDAO;
    private final OpcionVotacionDAO opcionDAO;
    private final MiembroDAO miembroDAO;

    public VotoService(VotoDAO votoDAO, VotacionDAO votacionDAO,
                       OpcionVotacionDAO opcionDAO, MiembroDAO miembroDAO) {
        this.votoDAO = votoDAO;
        this.votacionDAO = votacionDAO;
        this.opcionDAO = opcionDAO;
        this.miembroDAO = miembroDAO;
    }

    /**
     * Emite el voto del miembro de la sesion.
     *
     * <p>No lleva {@code synchronized}: la unicidad la garantizan la restriccion
     * {@code uk_voto_votacion_miembro} y el registro atomico del DAO. Un cerrojo de JVM
     * solo protegeria una instancia y daria una falsa sensacion de seguridad.
     */
    public EmitirVotoResponse emitirVoto(EmitirVotoRequest req, Integer idMiembroSesion) {
        if (req == null) {
            throw new IllegalArgumentException("La solicitud de voto es requerida.");
        }
        if (req.idVotacion() == null) {
            throw new IllegalArgumentException("El ID de votación es obligatorio.");
        }
        if (req.idOpcion() == null) {
            throw new IllegalArgumentException("Debe seleccionar una opción para votar.");
        }

        // La identidad del votante la fija SIEMPRE la sesion. El campo idMiembro del
        // cuerpo se conserva por compatibilidad del contrato, pero no decide: antes lo
        // sobrescribia, de modo que cualquier usuario autenticado podia votar en nombre
        // de otro miembro.
        if (idMiembroSesion == null) {
            throw new IllegalArgumentException("Se requiere una sesión con miembro asociado para emitir el voto.");
        }
        if (req.idMiembro() != null && !req.idMiembro().equals(idMiembroSesion)) {
            throw new IllegalStateException("No es posible emitir un voto en nombre de otro miembro.");
        }
        Integer idMiembro = idMiembroSesion;

        // 1. Validar miembro activo
        Miembro m = miembroDAO.findById(idMiembro)
            .orElseThrow(() -> new IllegalArgumentException("Miembro no encontrado con ID: " + idMiembro));
        if (m.getEstado() != Miembro.Estado.ACTIVO) {
            throw new IllegalStateException("Solo los miembros en estado ACTIVO tienen derecho al voto.");
        }

        // 2. Validar proceso de votación abierto
        Votacion v = votacionDAO.findById(req.idVotacion())
            .orElseThrow(() -> new IllegalArgumentException("Proceso de votación no encontrado con ID: " + req.idVotacion()));
        if (v.getEstado() != Votacion.Estado.ABIERTA) {
            throw new IllegalStateException("La votación no se encuentra en estado ABIERTA (estado actual: " + v.getEstado() + ").");
        }

        // 3. Validar opción correspondiente
        OpcionVotacion op = opcionDAO.findById(req.idOpcion())
            .orElseThrow(() -> new IllegalArgumentException("La opción de voto seleccionada no existe."));
        if (!op.getIdVotacion().equals(req.idVotacion())) {
            throw new IllegalArgumentException("La opción seleccionada no pertenece a este proceso de votación.");
        }

        // 4. Validar que no haya votado previamente
        if (votoDAO.existsByVotacionAndMiembro(req.idVotacion(), idMiembro)) {
            throw new IllegalStateException("El miembro ya ha emitido su voto en esta votación. Solo se permite un voto por persona.");
        }

        // 5. Registrar voto
        Voto nuevo = new Voto();
        nuevo.setIdVotacion(req.idVotacion());
        nuevo.setIdOpcion(req.idOpcion());
        nuevo.setIdMiembro(idMiembro);
        nuevo.setFechaHora(LocalDateTime.now());

        // El registro es atomico y la restriccion de unicidad decide. Si otro voto del
        // mismo miembro gana la carrera, la violacion llega como VotoDuplicadoException
        // y se traduce a conflicto de negocio; una PersistenciaException se propaga tal
        // cual, porque un fallo al guardar no puede presentarse como exito.
        Voto saved;
        try {
            saved = votoDAO.registrarUnico(nuevo);
        } catch (VotoDuplicadoException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }

        String fechaStr = DateUtils.formatDateTime(saved.getFechaHora());
        return new EmitirVotoResponse(saved.getIdVoto(), saved.getIdVotacion(), fechaStr, "Voto registrado exitosamente.");
    }

    public EstadoParticipacionResponse verificarParticipacion(Integer idVotacion, Integer idMiembro) {
        if (idVotacion == null || idMiembro == null) {
            return new EstadoParticipacionResponse(idVotacion, idMiembro, false, null);
        }

        Optional<Voto> votoOpt = votoDAO.findByVotacionAndMiembro(idVotacion, idMiembro);
        if (votoOpt.isPresent()) {
            Voto v = votoOpt.get();
            return new EstadoParticipacionResponse(idVotacion, idMiembro, true, DateUtils.formatDateTime(v.getFechaHora()));
        }
        return new EstadoParticipacionResponse(idVotacion, idMiembro, false, null);
    }
}
