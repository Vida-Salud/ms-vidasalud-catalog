package cl.duoc.ms_vidasalud_catalog.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ServicioResponse(
		Long id,
		String nombre,
		String descripcion,
		BigDecimal precio,
		Boolean activo,
		LocalDateTime fechaCreacion
) {
}
