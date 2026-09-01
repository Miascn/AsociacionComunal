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
import sv.asociacion.domain.entity.Asistencia;
import sv.asociacion.domain.entity.Miembro;
import sv.asociacion.domain.entity.Reunion;

public class AsistenciaService {
    private final AsistenciaDAO asistenciaDAO;
    private final ReunionDAO reunionDAO;
    private final MiembroDAO miembroDAO;

    public AsistenciaService(AsistenciaDAO asistenciaDAO, ReunionDAO reunionDAO, MiembroDAO miembroDAO) {
        this.asistenciaDAO = asistenciaDAO;
        this.reunionDAO = reunionDAO;
        this.miembroDAO = miembroDAO;
    }

    public List<AsistenciaResponse> getByReunion(Integer idReunion) {
        validarReunionExiste(idReunion);
        return asistenciaDAO.findByReunion(idReunion).stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    public List<AsistenciaResponse> convocarMiembros(Integer idReunion, ConvocatoriaMasivaRequest req) {
        Reunion r = validarReunionExiste(idReunion);
        if (r.getEstado() == Reunion.Estado.CANCELADA) {
            throw new IllegalStateException("No se pueden convocar miembros a una reunión en estado CANCELADA.");
        }

        List<Integer> targetIds = new ArrayList<>();
        if (req != null && req.miembrosIds() != null && !req.miembrosIds().isEmpty()) {
            targetIds.addAll(req.miembrosIds());
        } else {
            List<Miembro> activos = miembroDAO.findByEstado(Miembro.Estado.ACTIVO);
            for (Miembro m : activos) {
                targetIds.add(m.getIdMiembro());
            }
        }

        for (Integer idMiembro : targetIds) {
            if (!asistenciaDAO.existsByReunionAndMiembro(idReunion, idMiembro)) {
                Optional<Miembro> mOpt = miembroDAO.findById(idMiembro);
                if (mOpt.isPresent() && mOpt.get().getEstado() == Miembro.Estado.ACTIVO) {
                    Asistencia a = new Asistencia();
                    a.setIdReunion(idReunion);
                    a.setIdMiembro(idMiembro);
                    a.setAsistio(false);
                    a.setObservacion(null);
                    asistenciaDAO.save(a);
                }
            }
        }

        return getByReunion(idReunion);
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
