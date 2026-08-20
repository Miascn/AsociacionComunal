package sv.asociacion.domain.dto;

import sv.asociacion.domain.entity.Vivienda;

public record ViviendaResponse(
    Integer id, String codigo, String sector, String direccion, String referencia,
    Integer idRepresentante, String representante, String fechaRegistro, String estado,
    int adultos, int menores
) {
    public static ViviendaResponse from(Vivienda value) {
        return new ViviendaResponse(
            value.getIdVivienda(), value.getCodigo(), value.getSector(), value.getDireccion(),
            value.getReferencia(), value.getIdRepresentante(), value.getRepresentante(),
            value.getFechaRegistro() == null ? null : value.getFechaRegistro().toString(),
            value.getEstado(), value.getAdultos(), value.getMenores()
        );
    }
}
