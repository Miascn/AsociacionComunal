package sv.asociacion.domain.dto;

public record RolRequest(String nombre, String descripcion) {
    public String validationError() {
        if (nombre == null || nombre.isBlank()) {
            return "El nombre del rol es obligatorio.";
        }
        String trimmed = nombre.trim();
        if (trimmed.length() < 2 || trimmed.length() > 40) {
            return "El nombre del rol debe tener entre 2 y 40 caracteres.";
        }
        return null;
    }
}
