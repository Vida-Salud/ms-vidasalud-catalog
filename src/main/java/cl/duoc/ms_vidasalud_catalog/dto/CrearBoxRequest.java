package cl.duoc.ms_vidasalud_catalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CrearBoxRequest(
		@NotBlank(message = "nombre no puede estar vacío")
		String nombre,

		@NotNull(message = "servicioId es requerido")
		Long servicioId,

		@NotNull(message = "capacidadDiaria es requerida")
		@Positive(message = "capacidadDiaria debe ser positiva")
		Integer capacidadDiaria
) {
}
