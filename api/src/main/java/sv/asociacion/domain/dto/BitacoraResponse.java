package sv.asociacion.domain.dto;

import sv.asociacion.domain.entity.Bitacora;
import sv.asociacion.util.DateUtils;

public record BitacoraResponse(
    Long idBitacora,
    Integer idUsuario,
    String nombreUsuario,
    String accion,
    String entidad,
    String idRegistro,
    String fechaHora,
    String detalle
) {
    public static BitacoraResponse from(Bitacora b, String nombreUsuario) {
        return new BitacoraResponse(
            b.getIdBitacora(),
            b.getIdUsuario(),
            nombreUsuario,
            b.getAccion(),
            b.getEntidad(),
            b.getIdRegistro(),
            b.getFechaHora() != null ? DateUtils.formatDateTime(b.getFechaHora()) : null,
            b.getDetalle()
        );
    }
}
