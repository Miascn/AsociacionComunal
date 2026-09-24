package sv.asociacion.domain.dto;

import java.util.List;

public record ViviendaRequest(
    String codigo, String sector, String direccion, String referencia,
    Integer idRepresentante, String estado, List<String> adultos, List<String> menores
) {
    public String validationError() {
        if (codigo == null || codigo.trim().length() < 3) return "El código debe tener al menos 3 caracteres.";
        if (sector == null || sector.isBlank() || direccion == null || direccion.isBlank()) return "Sector y dirección son obligatorios.";
        if (idRepresentante != null && idRepresentante <= 0) return "El identificador del representante no es válido.";
        if (estado == null || !estado.matches("ACTIVA|DESHABITADA|INACTIVA")) return "El estado de la vivienda no es válido.";
        return null;
    }
}
