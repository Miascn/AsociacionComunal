package sv.asociacion.domain.dto;

import sv.asociacion.domain.entity.Proyecto;

public record CambioEstadoProyectoRequest(
    Proyecto.Estado nuevoEstado
) {}
