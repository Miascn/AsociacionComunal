package models;

import java.util.List;

public record BitacoraModel(
    Long idBitacora,
    Integer idUsuario,
    String nombreUsuario,
    String accion,
    String entidad,
    String idRegistro,
    String fechaHora,
    String detalle
) {
    public String getUsuarioDisplay() {
        if (nombreUsuario != null && !nombreUsuario.isBlank()) {
            return nombreUsuario;
        }
        return idUsuario != null ? "Usuario #" + idUsuario : "Sistema";
    }

    public String getDetalleCorto() {
        if (detalle == null) return "Sin detalle adicional";
        if (detalle.length() <= 80) return detalle;
        return detalle.substring(0, 77) + "...";
    }

    public record Page(
        List<BitacoraModel> items,
        int total,
        int page,
        int size,
        int totalPages
    ) {}
}