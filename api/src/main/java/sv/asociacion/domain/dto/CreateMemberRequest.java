package sv.asociacion.domain.dto;

public record CreateMemberRequest(
    String dui,
    String nombres,
    String apellidos,
    String telefono,
    String correo,
    String direccion
) {
    public String validationError() {
        if (dui == null || !dui.trim().matches("\\d{8}-\\d")) {
            return "El DUI debe tener el formato 00000000-0.";
        }
        if (nombres == null || nombres.isBlank() || apellidos == null || apellidos.isBlank()) {
            return "Los nombres y apellidos son obligatorios.";
        }
        if (direccion == null || direccion.isBlank()) {
            return "La direccion es obligatoria.";
        }
        if (telefono != null && !telefono.isBlank() && !telefono.trim().matches("\\d{4}-\\d{4}")) {
            return "El telefono debe tener el formato 0000-0000.";
        }
        if (correo != null && !correo.isBlank()
            && !correo.trim().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            return "El correo electronico no es valido.";
        }
        return null;
    }
}