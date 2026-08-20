package sv.asociacion.domain.dto;

import java.util.List;

public record ViviendaDetailResponse(ViviendaResponse vivienda, List<ResidentResponse> residentes) { }
