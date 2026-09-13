package cl.duoc.ms_vidasalud_catalog.dto;

import java.time.LocalDateTime;

public record BoxResponse(
		Long id,
		String nombre,
		Long servicioId,
		String servicioNombre,
		Integer capacidadDiaria,
		Boolean activo,
		LocalDateTime fechaCreacion
) {
}
