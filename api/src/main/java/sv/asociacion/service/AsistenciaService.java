package sv.asociacion.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import sv.asociacion.dao.AsistenciaDAO;
import sv.asociacion.dao.MiembroDAO;
import sv.asociacion.dao.ReunionDAO;
import sv.asociacion.domain.dto.AsistenciaRequest;
import sv.asociacion.domain.dto.AsistenciaResponse;
import sv.asociacion.domain.dto.ConvocatoriaMasivaRequest;
import sv.asociacion.domain.dto.ResultadoConvocatoria;
import sv.asociacion.domain.entity.Asistencia;
import sv.asociacion.domain.entity.Miembro;
import sv.asociacion.domain.entity.Reunion;

public class AsistenciaService {
    private final AsistenciaDAO asistenciaDAO;
    private final ReunionDAO reunionDAO;
    private final MiembroDAO miembroDAO;
    /** Proceso concurrente de convocatoria; depende de la interfaz ConvocatoriaRepository, no de JDBC. */
    private final ConvocatoriaConcurrenteService convocatoria;

    public AsistenciaService(AsistenciaDAO asistenciaDAO, ReunionDAO reunionDAO, MiembroDAO miembroDAO,
                             ConvocatoriaConcurrenteService convocatoria) {
        this.asistenciaDAO = asistenciaDAO;
        this.reunionDAO = reunionDAO;
        this.miembroDAO = miembroDAO;
        this.convocatoria = java.util.Objects.requireNonNull(convocatoria);
    }

    public List<AsistenciaResponse> getByReunion(Integer idReunion) {
        validarReunionExiste(idReunion);
        return asistenciaDAO.findByReunion(idReunion).stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    public List<AsistenciaResponse> getByMiembro(Integer idMiembro) {
        if (idMiembro == null) throw new IllegalArgumentException("El ID del miembro es obligatorio.");
        miembroDAO.findById(idMiembro)
            .orElseThrow(() -> new IllegalArgumentException("Miembro no encontrado con ID: " + idMiembro));
        return asistenciaDAO.findByMiembro(idMiembro).stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    /**
     * Convoca miembros a la reunion (todos los activos, o los indicados) y devuelve la
     * lista de asistencia resultante. Contrato usado por los clientes de escritorio y movil.
     */
    public List<AsistenciaResponse> convocarMiembros(Integer idReunion, ConvocatoriaMasivaRequest req) {
        convocarConResultado(idReunion, req);
        return getByReunion(idReunion);
    }

    /**
     * Igual que {@link #convocarMiembros} pero devuelve el resultado agregado del
     * proceso concurrente: miembros convocados, omitidos, lotes, hilos y duracion.
     */
    public ResultadoConvocatoria convocarConResultado(Integer idReunion, ConvocatoriaMasivaRequest req) {
        Reunion r = validarReunionExiste(idReunion);
        if (r.getEstado() == Reunion.Estado.CANCELADA) {
            throw new IllegalStateException("No se pueden convocar miembros a una reunión en estado CANCELADA.");
        }
        List<Integer> solicitados = req == null ? null : req.miembrosIds();
        return convocatoria.convocar(idReunion, solicitados);
    }

    public AsistenciaResponse registrarOActualizar(Integer idReunion, AsistenciaRequest req) {
        Reunion r = validarReunionExiste(idReunion);
        if (r.getEstado() == Reunion.Estado.CANCELADA) {
            throw new IllegalStateException("No se pueden registrar asistencias en una reunión en estado CANCELADA.");
        }

        if (req == null || req.idMiembro() == null) {
            throw new IllegalArgumentException("El ID del miembro es obligatorio.");
        }

        Miembro m = miembroDAO.findById(req.idMiembro())
            .orElseThrow(() -> new IllegalArgumentException("Miembro no encontrado con ID: " + req.idMiembro()));

        String obs = req.observacion() != null ? req.observacion().trim() : null;
        if (obs != null && obs.length() > 200) {
            throw new IllegalArgumentException("La observación no puede superar los 200 caracteres.");
        }

        Optional<Asistencia> existenteOpt = asistenciaDAO.findByReunionAndMiembro(idReunion, req.idMiembro());
        Asistencia result;
        if (existenteOpt.isPresent()) {
            Asistencia a = existenteOpt.get();
            a.setAsistio(req.asistio());
            a.setObservacion(obs);
            result = asistenciaDAO.update(a);
        } else {
            Asistencia a = new Asistencia();
            a.setIdReunion(idReunion);
            a.setIdMiembro(req.idMiembro());
            a.setAsistio(req.asistio());
            a.setObservacion(obs);
            result = asistenciaDAO.save(a);
        }

        return toResponse(result);
    }

    public List<AsistenciaResponse> registrarLote(Integer idReunion, List<AsistenciaRequest> lote) {
        Reunion r = validarReunionExiste(idReunion);
        if (r.getEstado() == Reunion.Estado.CANCELADA) {
            throw new IllegalStateException("No se pueden registrar asistencias en una reunión en estado CANCELADA.");
        }

        if (lote == null || lote.isEmpty()) {
            throw new IllegalArgumentException("La lista de asistencias no puede estar vacía.");
        }

        java.util.Set<Integer> vistos = new java.util.HashSet<>();
        List<Asistencia> asistencias = new ArrayList<>();

        for (AsistenciaRequest req : lote) {
            if (req == null || req.idMiembro() == null) {
                throw new IllegalArgumentException("El ID del miembro es obligatorio.");
            }
            if (!vistos.add(req.idMiembro())) {
                throw new IllegalArgumentException("ID de miembro duplicado en el lote: " + req.idMiembro());
            }

            Optional<Miembro> mOpt = miembroDAO.findById(req.idMiembro());
            if (mOpt.isEmpty()) {
                throw new IllegalArgumentException("Miembro no encontrado con ID: " + req.idMiembro());
            }

            String obs = req.observacion() != null ? req.observacion().trim() : null;
            if (obs != null && obs.length() > 200) {
                throw new IllegalArgumentException("La observación no puede superar los 200 caracteres.");
            }

            Asistencia a = new Asistencia();
            a.setIdReunion(idReunion);
            a.setIdMiembro(req.idMiembro());
            a.setAsistio(req.asistio());
            a.setObservacion(obs);
            asistencias.add(a);
        }

        try {
            asistenciaDAO.saveLoteTransaccional(idReunion, asistencias);
        } catch (java.sql.SQLException e) {
            // El mensaje del servidor de BD no se concatena: queda como causa, no llega al cliente.
            throw sv.asociacion.exception.TraductorSQLException.traducir("guardar el lote de asistencias", e);
        }

        return getByReunion(idReunion);
    }

    public AsistenciaResponse toggleAsistencia(Long idAsistencia, boolean asistio, String observacion) {
        Asistencia a = asistenciaDAO.findById(idAsistencia)
            .orElseThrow(() -> new IllegalArgumentException("Registro de asistencia no encontrado con ID: " + idAsistencia));

        Reunion r = validarReunionExiste(a.getIdReunion());
        if (r.getEstado() == Reunion.Estado.CANCELADA) {
            throw new IllegalStateException("No se puede modificar la asistencia de una reunión CANCELADA.");
        }

        String obs = observacion != null ? observacion.trim() : null;
        if (obs != null && obs.length() > 200) {
            throw new IllegalArgumentException("La observación no puede superar los 200 caracteres.");
        }

        a.setAsistio(asistio);
        a.setObservacion(obs);
        asistenciaDAO.update(a);
        return toResponse(a);
    }

    public void delete(Long idAsistencia) {
        Asistencia a = asistenciaDAO.findById(idAsistencia)
            .orElseThrow(() -> new IllegalArgumentException("Registro de asistencia no encontrado con ID: " + idAsistencia));

        Reunion r = validarReunionExiste(a.getIdReunion());
        if (r.getEstado() == Reunion.Estado.REALIZADA) {
            throw new IllegalStateException("No se puede eliminar un registro de asistencia de una reunión ya REALIZADA.");
        }

        asistenciaDAO.delete(idAsistencia);
    }

    private Reunion validarReunionExiste(Integer idReunion) {
        if (idReunion == null) throw new IllegalArgumentException("El ID de reunión es obligatorio.");
        return reunionDAO.findById(idReunion)
            .orElseThrow(() -> new IllegalArgumentException("Reunión no encontrada con ID: " + idReunion));
    }

    private AsistenciaResponse toResponse(Asistencia a) {
        String nombre = "Miembro #" + a.getIdMiembro();
        String dui = "-";
        String tel = "-";

        Optional<Miembro> mOpt = miembroDAO.findById(a.getIdMiembro());
        if (mOpt.isPresent()) {
            Miembro m = mOpt.get();
            nombre = (m.getNombres() + " " + m.getApellidos()).trim();
            if (m.getDui() != null && !m.getDui().isBlank()) dui = m.getDui();
            if (m.getTelefono() != null && !m.getTelefono().isBlank()) tel = m.getTelefono();
        }

        return new AsistenciaResponse(
            a.getIdAsistencia(),
            a.getIdReunion(),
            a.getIdMiembro(),
            nombre,
            dui,
            tel,
            a.isAsistio(),
            a.getObservacion()
        );
    }
}
