package sv.asociacion.service;

import java.time.LocalDateTime;
import java.util.List;
import sv.asociacion.dao.BitacoraDAO;
import sv.asociacion.domain.dto.BitacoraPageResponse;
import sv.asociacion.domain.dto.BitacoraResponse;
import sv.asociacion.domain.entity.Bitacora;

public class BitacoraService {
    private final BitacoraDAO bitacoraDAO;

    public BitacoraService(BitacoraDAO bitacoraDAO) {
        this.bitacoraDAO = bitacoraDAO;
    }

    public Bitacora registrar(Integer idUsuario, String accion, String entidad, String idRegistro, String detalle) {
        try {
            Bitacora b = new Bitacora();
            b.setIdUsuario(idUsuario != null ? idUsuario : 1);
            b.setAccion(accion != null ? accion.trim().toUpperCase() : "GENERAL");
            b.setEntidad(entidad != null ? entidad.trim().toUpperCase() : "SISTEMA");
            b.setIdRegistro(idRegistro != null ? idRegistro.trim() : null);
            b.setFechaHora(LocalDateTime.now());
            b.setDetalle(detalle != null ? detalle.trim() : null);
            return bitacoraDAO.save(b);
        } catch (Exception e) {
            System.err.println("Error al registrar evento de auditoría en bitácora: " + e.getMessage());
            return null;
        }
    }

    public BitacoraPageResponse findPage(String usuario, String entidad, String accion,
                                         String fechaDesde, String fechaHasta,
                                         int page, int size) {
        int safePage = Math.max(1, page);
        int safeSize = size <= 0 ? 20 : Math.min(size, 100);
        int offset = (safePage - 1) * safeSize;

        int total = bitacoraDAO.countFiltered(usuario, entidad, accion, fechaDesde, fechaHasta);
        List<BitacoraDAO.BitacoraEntry> entries = bitacoraDAO.findFiltered(
            usuario, entidad, accion, fechaDesde, fechaHasta, offset, safeSize
        );

        List<BitacoraResponse> items = entries.stream()
            .map(e -> BitacoraResponse.from(e.bitacora(), e.nombreUsuario()))
            .toList();

        return BitacoraPageResponse.of(items, total, safePage, safeSize);
    }

    public BitacoraResponse findById(Long id) {
        if (id == null) return null;
        return bitacoraDAO.findByIdWithUser(id)
            .map(e -> BitacoraResponse.from(e.bitacora(), e.nombreUsuario()))
            .orElse(null);
    }
}
