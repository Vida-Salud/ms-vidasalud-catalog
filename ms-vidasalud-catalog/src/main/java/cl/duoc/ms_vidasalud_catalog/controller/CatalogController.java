package cl.duoc.ms_vidasalud_catalog.controller;

import cl.duoc.ms_vidasalud_catalog.dto.ActualizarCupoRequest;
import cl.duoc.ms_vidasalud_catalog.dto.ActualizarServicioRequest;
import cl.duoc.ms_vidasalud_catalog.dto.BoxResponse;
import cl.duoc.ms_vidasalud_catalog.dto.CrearBoxRequest;
import cl.duoc.ms_vidasalud_catalog.dto.CrearServicioRequest;
import cl.duoc.ms_vidasalud_catalog.dto.CupoDisponibleResponse;
import cl.duoc.ms_vidasalud_catalog.dto.ServicioResponse;
import cl.duoc.ms_vidasalud_catalog.service.CatalogService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/catalog")
public class CatalogController {

	@Autowired
	private CatalogService catalogService;

	// === SERVICIOS ===

	@PostMapping("/services")
	public ResponseEntity<ServicioResponse> crearServicio(
			@Valid @RequestBody CrearServicioRequest request) {

		ServicioResponse creado = catalogService.crearServicio(request);

		URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest()
				.path("/{id}")
				.buildAndExpand(creado.id())
				.toUri();

		return ResponseEntity.created(ubicacion).body(creado);
	}

	@GetMapping("/services")
	public ResponseEntity<List<ServicioResponse>> listarServicios() {
		return ResponseEntity.ok(catalogService.listarServicios());
	}

	@GetMapping("/services/{id}")
	public ResponseEntity<ServicioResponse> obtenerServicio(@PathVariable Long id) {
		return ResponseEntity.ok(catalogService.obtenerServicio(id));
	}

	@PutMapping("/services/{id}")
	public ResponseEntity<ServicioResponse> actualizarServicio(
			@PathVariable Long id,
			@Valid @RequestBody ActualizarServicioRequest request) {

		return ResponseEntity.ok(catalogService.actualizarServicio(id, request));
	}

	// === BOXES ===

	@PostMapping("/boxes")
	public ResponseEntity<BoxResponse> crearBox(
			@Valid @RequestBody CrearBoxRequest request) {

		BoxResponse creado = catalogService.crearBox(request);

		URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest()
				.path("/{id}")
				.buildAndExpand(creado.id())
				.toUri();

		return ResponseEntity.created(ubicacion).body(creado);
	}

	@GetMapping("/boxes")
	public ResponseEntity<List<BoxResponse>> listarBoxes(
			@RequestParam(required = false) Long servicioId) {

		if (servicioId != null) {
			return ResponseEntity.ok(catalogService.listarBoxesPorServicio(servicioId));
		}
		return ResponseEntity.ok(catalogService.listarBoxes());
	}

	@GetMapping("/boxes/{id}")
	public ResponseEntity<BoxResponse> obtenerBox(@PathVariable Long id) {
		return ResponseEntity.ok(catalogService.obtenerBox(id));
	}

	// === CUPOS ===

	@GetMapping("/cupos")
	public ResponseEntity<List<CupoDisponibleResponse>> listarCupos(
			@RequestParam(required = false) LocalDate fecha) {

		if (fecha != null) {
			return ResponseEntity.ok(catalogService.listarCuposPorFecha(fecha));
		}
		return ResponseEntity.ok(catalogService.listarCuposPorFecha(LocalDate.now()));
	}

	@GetMapping("/cupos/box/{boxId}/fecha/{fecha}")
	public ResponseEntity<CupoDisponibleResponse> obtenerCupoPorBoxYFecha(
			@PathVariable Long boxId,
			@PathVariable LocalDate fecha) {

		return ResponseEntity.ok(catalogService.obtenerCupoPorBoxYFecha(boxId, fecha));
	}

	@PutMapping("/cupos/{id}")
	public ResponseEntity<CupoDisponibleResponse> actualizarCupo(
			@PathVariable Long id,
			@Valid @RequestBody ActualizarCupoRequest request) {

		return ResponseEntity.ok(catalogService.actualizarCupo(id, request.cuposDisponibles()));
	}
}
