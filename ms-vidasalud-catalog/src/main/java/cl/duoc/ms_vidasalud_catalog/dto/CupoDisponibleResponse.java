package cl.duoc.ms_vidasalud_catalog.dto;

import java.time.LocalDate;

public record CupoDisponibleResponse(
		Long id,
		Long boxId,
		String boxNombre,
		LocalDate fecha,
		Integer cuposDisponibles
) {
}
