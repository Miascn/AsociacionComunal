package sv.asociacion.domain.dto;

import java.util.List;

public record AsistenciaLoteRequest(
    List<AsistenciaRequest> asistencias
) {}
