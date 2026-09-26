package sv.asociacion.domain.dto;

import java.math.BigDecimal;

public record ConfiguracionCuotaDto(
    BigDecimal cuotaMantenimiento,
    String descripcion
) {}
