package sv.asociacion.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import sv.asociacion.dao.AsistenciaDAO;
import sv.asociacion.dao.ReunionDAO;
import sv.asociacion.domain.dto.ReunionRequest;
import sv.asociacion.domain.dto.ReunionResponse;
import sv.asociacion.domain.entity.Reunion;
import sv.asociacion.util.DateUtils;

public class ReunionService {
    private final ReunionDAO reunionDAO;
    private final AsistenciaDAO asistenciaDAO;

    public ReunionService(ReunionDAO reunionDAO, AsistenciaDAO asistenciaDAO) {
        this.reunionDAO = reunionDAO;
        this.asistenciaDAO = asistenciaDAO;
    }

    public List<ReunionResponse> getAll(String search, String tipoStr, String estadoStr) {
        return getAll(search, tipoStr, estadoStr, null, null);
    }

    public List<ReunionResponse> getAll(String search, String tipoStr, String estadoStr, String desde, String hasta) {
        Reunion.Tipo tipo = null;
        if (tipoStr != null && !tipoStr.isBlank()) {
            try {
                tipo = Reunion.Tipo.valueOf(tipoStr.trim().toUpperCase());
            } catch (Exception ignored) {}
        }

        Reunion.Estado estado = null;
        if (estadoStr != null && !estadoStr.isBlank()) {
            try {
                estado = Reunion.Estado.valueOf(estadoStr.trim().toUpperCase());
            } catch (Exception ignored) {}
        }

        return reunionDAO.findFiltered(search, tipo, estado, desde, hasta).stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    public ReunionResponse getById(Integer id) {
        Reunion r = reunionDAO.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Reunión no encontrada con ID: " + id));
        return toResponse(r);
    }

    public ReunionResponse create(ReunionRequest req) {
        validarRequest(req);

        LocalDateTime fechaHora;
        try {
            fechaHora = DateUtils.parseDateTime(req.fechaHora());
        } catch (Exception e) {
            throw new IllegalArgumentException("El formato de fecha y hora es inválido (debe ser yyyy-MM-dd HH:mm:ss o ISO-8601).");
        }
        if (fechaHora == null) {
            throw new IllegalArgumentException("El formato de fecha y hora es inválido (debe ser yyyy-MM-dd HH:mm:ss o ISO-8601).");
        }

        Reunion.Tipo tipo = parseTipo(req.tipo());

        Reunion r = new Reunion();
        r.setTitulo(req.titulo().trim());
        r.setFechaHora(fechaHora);
        r.setLugar(req.lugar() != null ? req.lugar().trim() : null);
        r.setTipo(tipo);
        r.setEstado(Reunion.Estado.PROGRAMADA);

        Reunion saved = reunionDAO.save(r);
        return toResponse(saved);
    }

    public ReunionResponse update(Integer id, ReunionRequest req) {
        Reunion r = reunionDAO.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Reunión no encontrada con ID: " + id));

        if (r.getEstado() != Reunion.Estado.PROGRAMADA) {
            throw new IllegalStateException("No se pueden modificar los detalles de una reunión en estado " + r.getEstado() + ".");
        }

        validarRequest(req);

        LocalDateTime fechaHora;
        try {
            fechaHora = DateUtils.parseDateTime(req.fechaHora());
        } catch (Exception e) {
            throw new IllegalArgumentException("El formato de fecha y hora es inválido.");
        }
        if (fechaHora == null) {
            throw new IllegalArgumentException("El formato de fecha y hora es inválido.");
        }

        r.setTitulo(req.titulo().trim());
        r.setFechaHora(fechaHora);
        r.setLugar(req.lugar() != null ? req.lugar().trim() : null);
        r.setTipo(parseTipo(req.tipo()));

        Reunion updated = reunionDAO.update(r);
        return toResponse(updated);
    }

    public ReunionResponse marcarRealizada(Integer id) {
        Reunion r = reunionDAO.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Reunión no encontrada con ID: " + id));

        if (r.getEstado() != Reunion.Estado.PROGRAMADA) {
            throw new IllegalStateException("Solo una reunión PROGRAMADA puede marcarse como REALIZADA (estado actual: " + r.getEstado() + ").");
        }

        r.setEstado(Reunion.Estado.REALIZADA);
        reunionDAO.updateEstado(id, Reunion.Estado.REALIZADA);
        return toResponse(r);
    }

    public ReunionResponse cancelar(Integer id) {
        Reunion r = reunionDAO.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Reunión no encontrada con ID: " + id));

        if (r.getEstado() == Reunion.Estado.REALIZADA) {
            throw new IllegalStateException("No se puede cancelar una reunión que ya fue REALIZADA.");
        }
        if (r.getEstado() == Reunion.Estado.CANCELADA) {
            throw new IllegalStateException("La reunión ya se encuentra CANCELADA.");
        }

        r.setEstado(Reunion.Estado.CANCELADA);
        reunionDAO.updateEstado(id, Reunion.Estado.CANCELADA);
        return toResponse(r);
    }

    public void delete(Integer id) {
        Reunion r = reunionDAO.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Reunión no encontrada con ID: " + id));

        if (r.getEstado() == Reunion.Estado.REALIZADA) {
            throw new IllegalStateException("No se puede eliminar una reunión que ya fue realizada.");
        }

        int totalAsistencias = asistenciaDAO.countByReunion(id);
        if (totalAsistencias > 0) {
            throw new IllegalStateException("No se puede eliminar la reunión porque tiene " + totalAsistencias + " registros de asistencia. Use la opción de cancelar.");
        }

        reunionDAO.delete(id);
    }

    private void validarRequest(ReunionRequest req) {
        if (req == null) throw new IllegalArgumentException("La solicitud no puede ser nula.");
        if (req.titulo() == null || req.titulo().trim().isBlank()) {
            throw new IllegalArgumentException("El título de la reunión es obligatorio.");
        }
        if (req.titulo().trim().length() > 150) {
            throw new IllegalArgumentException("El título no puede superar los 150 caracteres.");
        }
        if (req.fechaHora() == null || req.fechaHora().trim().isBlank()) {
            throw new IllegalArgumentException("La fecha y hora de la reunión son obligatorias.");
        }
        if (req.lugar() != null && req.lugar().trim().length() > 150) {
            throw new IllegalArgumentException("El lugar no puede superar los 150 caracteres.");
        }
    }

    private Reunion.Tipo parseTipo(String tipoStr) {
        if (tipoStr == null || tipoStr.isBlank()) return Reunion.Tipo.ORDINARIA;
        try {
            return Reunion.Tipo.valueOf(tipoStr.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Tipo de reunión inválido. Valores permitidos: ORDINARIA, EXTRAORDINARIA.");
        }
    }

    private ReunionResponse toResponse(Reunion r) {
        int totalConvocados = asistenciaDAO.countByReunion(r.getIdReunion());
        int totalAsistentes = asistenciaDAO.countAsistieronByReunion(r.getIdReunion());
        double porcentaje = totalConvocados > 0 ? (totalAsistentes * 100.0) / totalConvocados : 0.0;
        boolean editable = r.getEstado() == Reunion.Estado.PROGRAMADA;

        return new ReunionResponse(
            r.getIdReunion(),
            r.getTitulo(),
            DateUtils.formatDateTime(r.getFechaHora()),
            r.getLugar(),
            r.getTipo().name(),
            r.getEstado().name(),
            totalConvocados,
            totalAsistentes,
            Math.round(porcentaje * 10.0) / 10.0,
            editable
        );
    }
}
