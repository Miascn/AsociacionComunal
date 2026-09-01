package sv.asociacion.domain.dto;

import java.time.LocalDateTime;
import java.util.List;

public record VotacionRequest(
    String titulo,
    String descripcion,
    Integer idProyecto,
    LocalDateTime fechaInicio,
    LocalDateTime fechaFin,
    List<String> opcionesIniciales
) {}
