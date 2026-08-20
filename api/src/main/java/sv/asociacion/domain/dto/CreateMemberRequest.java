package sv.asociacion.domain.dto;

public record CreateMemberRequest(
    String documento,
    String tipoDocumento,
    String paisOrigen,
    String nombres,
    String apellidos,
    String telefono,
    String correo,
    Integer idVivienda
) {
    public String validationError() {
        String type = tipoDocumento == null ? "" : tipoDocumento.trim();
        String document = documento == null ? "" : documento.trim();
        if (!type.matches("DUI|PASAPORTE|CARNET_RESIDENTE")) return "El tipo de documento no es válido.";
        if ("DUI".equals(type) && !document.matches("\\d{9}")) return "El DUI debe contener 9 dígitos.";
        if (!"DUI".equals(type) && !document.matches("[A-Za-z0-9]{5,30}")) return "El documento debe contener entre 5 y 30 letras o números.";
        if (!"DUI".equals(type) && (paisOrigen == null || paisOrigen.isBlank())) return "El país de origen es obligatorio.";
        if (nombres == null || nombres.isBlank() || apellidos == null || apellidos.isBlank()) return "Los nombres y apellidos son obligatorios.";
        if (idVivienda == null || idVivienda <= 0) return "La vivienda es obligatoria.";
        if (correo != null && !correo.isBlank() && !correo.trim().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) return "El correo electrónico no es válido.";
        return null;
    }
}
