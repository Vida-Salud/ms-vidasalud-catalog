package cl.duoc.ms_vidasalud_catalog.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record ActualizarCupoRequest(
		@NotNull(message = "cuposDisponibles es requerido")
		@PositiveOrZero(message = "cuposDisponibles no puede ser negativo")
		Integer cuposDisponibles
) {
}
