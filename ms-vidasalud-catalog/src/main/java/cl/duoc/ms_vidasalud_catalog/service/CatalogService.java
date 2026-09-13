package cl.duoc.ms_vidasalud_catalog.service;

import cl.duoc.ms_vidasalud_catalog.dto.ActualizarServicioRequest;
import cl.duoc.ms_vidasalud_catalog.dto.BoxResponse;
import cl.duoc.ms_vidasalud_catalog.dto.CrearBoxRequest;
import cl.duoc.ms_vidasalud_catalog.dto.CrearServicioRequest;
import cl.duoc.ms_vidasalud_catalog.dto.CupoDisponibleResponse;
import cl.duoc.ms_vidasalud_catalog.dto.ServicioResponse;
import cl.duoc.ms_vidasalud_catalog.model.Box;
import cl.duoc.ms_vidasalud_catalog.model.CupoDisponible;
import cl.duoc.ms_vidasalud_catalog.model.Servicio;
import cl.duoc.ms_vidasalud_catalog.repository.BoxRepository;
import cl.duoc.ms_vidasalud_catalog.repository.CupoDisponibleRepository;
import cl.duoc.ms_vidasalud_catalog.repository.ServicioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@Service
public class CatalogService {

	@Autowired
	private ServicioRepository servicioRepository;

	@Autowired
	private BoxRepository boxRepository;

	@Autowired
	private CupoDisponibleRepository cupoRepository;

	// === SERVICIOS ===

	public ServicioResponse crearServicio(CrearServicioRequest request) {
		Servicio servicio = new Servicio(
				normalizar(request.nombre()),
				normalizar(request.descripcion()),
				request.precio()
		);
		return toServicioResponse(servicioRepository.save(servicio));
	}

	public List<ServicioResponse> listarServicios() {
		return servicioRepository.findAll()
				.stream()
				.map(this::toServicioResponse)
				.toList();
	}

	public ServicioResponse obtenerServicio(Long id) {
		return toServicioResponse(buscarServicio(id));
	}

	public ServicioResponse actualizarServicio(Long id, ActualizarServicioRequest request) {
		Servicio servicio = buscarServicio(id);

		if (request.nombre() != null) {
			servicio.setNombre(normalizar(request.nombre()));
		}
		if (request.descripcion() != null) {
			servicio.setDescripcion(normalizar(request.descripcion()));
		}
		if (request.precio() != null) {
			servicio.setPrecio(request.precio());
		}

		return toServicioResponse(servicioRepository.save(servicio));
	}

	// === BOXES ===

	public BoxResponse crearBox(CrearBoxRequest request) {
		Servicio servicio = buscarServicio(request.servicioId());

		Box box = new Box(
				normalizar(request.nombre()),
				servicio,
				request.capacidadDiaria()
		);
		boxRepository.save(box);

		// Inicializar cupos para 30 días adelante
		inicializarCuposParaBox(box);

		return toBoxResponse(box);
	}

	public List<BoxResponse> listarBoxes() {
		return boxRepository.findAll()
				.stream()
				.map(this::toBoxResponse)
				.toList();
	}

	public List<BoxResponse> listarBoxesPorServicio(Long servicioId) {
		buscarServicio(servicioId); // validar que existe
		return boxRepository.findByServicioId(servicioId)
				.stream()
				.map(this::toBoxResponse)
				.toList();
	}

	public BoxResponse obtenerBox(Long id) {
		return toBoxResponse(buscarBox(id));
	}

	// === CUPOS ===

	public List<CupoDisponibleResponse> listarCuposPorFecha(LocalDate fecha) {
		return cupoRepository.findByFecha(fecha)
				.stream()
				.map(this::toCupoResponse)
				.toList();
	}

	public CupoDisponibleResponse obtenerCupoPorBoxYFecha(Long boxId, LocalDate fecha) {
		buscarBox(boxId); // validar que existe
		CupoDisponible cupo = cupoRepository.findByBoxIdAndFecha(boxId, fecha)
				.orElseThrow(() -> new ResponseStatusException(
						HttpStatus.NOT_FOUND,
						"No existe cupo para box " + boxId + " en fecha " + fecha
				));
		return toCupoResponse(cupo);
	}

	public CupoDisponibleResponse actualizarCupo(Long cupoId, Integer nuevosCupos) {
		CupoDisponible cupo = cupoRepository.findById(cupoId)
				.orElseThrow(() -> new ResponseStatusException(
						HttpStatus.NOT_FOUND,
						"Cupo no encontrado: " + cupoId
				));

		cupo.setCuposDisponibles(nuevosCupos);
		return toCupoResponse(cupoRepository.save(cupo));
	}

	public void decrementarCupo(Long boxId, LocalDate fecha) {
		CupoDisponible cupo = cupoRepository.findByBoxIdAndFecha(boxId, fecha)
				.orElseThrow(() -> new ResponseStatusException(
						HttpStatus.NOT_FOUND,
						"No hay cupo para ese box y fecha"
				));

		if (!cupo.tieneCupoDisponible()) {
			throw new ResponseStatusException(
					HttpStatus.BAD_REQUEST,
					"Sin cupos disponibles para box " + boxId + " en " + fecha
			);
		}

		cupo.decrementarCupo();
		cupoRepository.save(cupo);
	}

	// === PRIVADOS ===

	private Servicio buscarServicio(Long id) {
		return servicioRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(
						HttpStatus.NOT_FOUND,
						"Servicio no encontrado: " + id
				));
	}

	private Box buscarBox(Long id) {
		return boxRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(
						HttpStatus.NOT_FOUND,
						"Box no encontrado: " + id
				));
	}

	private void inicializarCuposParaBox(Box box) {
		LocalDate hoy = LocalDate.now();
		for (int i = 0; i < 30; i++) {
			LocalDate fecha = hoy.plusDays(i);
			if (cupoRepository.findByBoxIdAndFecha(box.getId(), fecha).isEmpty()) {
				CupoDisponible cupo = new CupoDisponible(
						box,
						fecha,
						box.getCapacidadDiaria()
				);
				cupoRepository.save(cupo);
			}
		}
	}

	private ServicioResponse toServicioResponse(Servicio servicio) {
		return new ServicioResponse(
				servicio.getId(),
				servicio.getNombre(),
				servicio.getDescripcion(),
				servicio.getPrecio(),
				servicio.getActivo(),
				servicio.getFechaCreacion()
		);
	}

	private BoxResponse toBoxResponse(Box box) {
		return new BoxResponse(
				box.getId(),
				box.getNombre(),
				box.getServicio().getId(),
				box.getServicio().getNombre(),
				box.getCapacidadDiaria(),
				box.getActivo(),
				box.getFechaCreacion()
		);
	}

	private CupoDisponibleResponse toCupoResponse(CupoDisponible cupo) {
		return new CupoDisponibleResponse(
				cupo.getId(),
				cupo.getBox().getId(),
				cupo.getBox().getNombre(),
				cupo.getFecha(),
				cupo.getCuposDisponibles()
		);
	}

	private String normalizar(String valor) {
		if (valor == null) {
			return null;
		}
		String limpio = valor.trim();
		return limpio.isEmpty() ? null : limpio;
	}
}
