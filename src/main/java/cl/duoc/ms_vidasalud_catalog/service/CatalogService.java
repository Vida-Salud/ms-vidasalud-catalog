package cl.duoc.ms_vidasalud_catalog.service;

import cl.duoc.ms_vidasalud_catalog.dto.ActualizarServicioRequest;
import cl.duoc.ms_vidasalud_catalog.dto.BoxResponse;
import cl.duoc.ms_vidasalud_catalog.dto.CrearBoxRequest;
import cl.duoc.ms_vidasalud_catalog.dto.CrearServicioRequest;
import cl.duoc.ms_vidasalud_catalog.dto.CupoDisponibleResponse;
import cl.duoc.ms_vidasalud_catalog.dto.ServicioResponse;

import java.time.LocalDate;
import java.util.List;

public interface CatalogService {
	ServicioResponse crearServicio(CrearServicioRequest request);
	List<ServicioResponse> listarServicios();
	ServicioResponse obtenerServicio(Long id);
	ServicioResponse actualizarServicio(Long id, ActualizarServicioRequest request);
	BoxResponse crearBox(CrearBoxRequest request);
	List<BoxResponse> listarBoxes();
	List<BoxResponse> listarBoxesPorServicio(Long servicioId);
	BoxResponse obtenerBox(Long id);
	List<CupoDisponibleResponse> listarCupos(LocalDate fecha, Long boxId);
	CupoDisponibleResponse actualizarCupo(Long cupoId, Integer nuevosCupos);
	void decrementarCupo(Long boxId, LocalDate fecha);
}
