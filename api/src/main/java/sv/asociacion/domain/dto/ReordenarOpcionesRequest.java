package sv.asociacion.domain.dto;

import java.util.List;

public record ReordenarOpcionesRequest(
    List<Integer> idsEnOrden
) {}
