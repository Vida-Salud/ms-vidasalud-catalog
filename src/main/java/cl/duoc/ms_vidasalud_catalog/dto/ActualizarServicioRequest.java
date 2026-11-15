package cl.duoc.ms_vidasalud_catalog.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record ActualizarServicioRequest(
		String nombre,
		String descripcion,

		@NotNull(message = "precio es requerido")
		@Positive(message = "precio debe ser positivo")
		BigDecimal precio
) {
}
