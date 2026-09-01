package sv.asociacion.domain.dto;

import java.util.List;

public record ConvocatoriaMasivaRequest(
    List<Integer> miembrosIds
) {}
