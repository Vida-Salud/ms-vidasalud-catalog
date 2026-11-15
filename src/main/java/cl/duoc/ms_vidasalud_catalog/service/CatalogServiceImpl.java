package cl.duoc.ms_vidasalud_catalog.service;

import cl.duoc.ms_vidasalud_catalog.dto.ActualizarServicioRequest;
import cl.duoc.ms_vidasalud_catalog.dto.BoxResponse;
import cl.duoc.ms_vidasalud_catalog.dto.CrearBoxRequest;
import cl.duoc.ms_vidasalud_catalog.dto.CrearServicioRequest;
import cl.duoc.ms_vidasalud_catalog.dto.CupoDisponibleResponse;
import cl.duoc.ms_vidasalud_catalog.dto.ServicioResponse;
import cl.duoc.ms_vidasalud_catalog.exception.RecursoNoEncontradoException;
import cl.duoc.ms_vidasalud_catalog.exception.ReglaNegocioException;
import cl.duoc.ms_vidasalud_catalog.model.Box;
import cl.duoc.ms_vidasalud_catalog.model.CupoDisponible;
import cl.duoc.ms_vidasalud_catalog.model.Servicio;
import cl.duoc.ms_vidasalud_catalog.repository.BoxRepository;
import cl.duoc.ms_vidasalud_catalog.repository.CupoDisponibleRepository;
import cl.duoc.ms_vidasalud_catalog.repository.ServicioRepository;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@Service
public class CatalogServiceImpl implements CatalogService {

	private final ServicioRepository servicioRepository;
	private final BoxRepository boxRepository;
	private final CupoDisponibleRepository cupoRepository;
	private final Clock clock;

	public CatalogServiceImpl(ServicioRepository servicioRepository, BoxRepository boxRepository,
			CupoDisponibleRepository cupoRepository, Clock clock) {
		this.servicioRepository = servicioRepository;
		this.boxRepository = boxRepository;
		this.cupoRepository = cupoRepository;
		this.clock = clock;
	}

	// === SERVICIOS ===

	@Override
	public ServicioResponse crearServicio(CrearServicioRequest request) {
		Servicio servicio = new Servicio(
				normalizar(request.nombre()),
				normalizar(request.descripcion()),
				request.precio()
		);
		return toServicioResponse(servicioRepository.save(servicio));
	}

	@Override
	public List<ServicioResponse> listarServicios() {
		return servicioRepository.findAll()
				.stream()
				.map(this::toServicioResponse)
				.toList();
	}

	@Override
	public ServicioResponse obtenerServicio(Long id) {
		return toServicioResponse(buscarServicio(id));
	}

	@Override
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

	@Override
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

	@Override
	public List<BoxResponse> listarBoxes() {
		return boxRepository.findAll()
				.stream()
				.map(this::toBoxResponse)
				.toList();
	}

	@Override
	public List<BoxResponse> listarBoxesPorServicio(Long servicioId) {
		buscarServicio(servicioId); // validar que existe
		return boxRepository.findByServicioId(servicioId)
				.stream()
				.map(this::toBoxResponse)
				.toList();
	}

	@Override
	public BoxResponse obtenerBox(Long id) {
		return toBoxResponse(buscarBox(id));
	}

	// === CUPOS ===

	@Override
	public List<CupoDisponibleResponse> listarCupos(LocalDate fecha, Long boxId) {
		LocalDate dia = fecha != null ? fecha : LocalDate.now(clock);
		List<CupoDisponible> cupos = boxId != null
				? cupoRepository.findByBoxIdAndFecha(boxId, dia).stream().toList()
				: cupoRepository.findByFecha(dia);
		return cupos.stream().map(this::toCupoResponse).toList();
	}

	@Override
	public CupoDisponibleResponse actualizarCupo(Long cupoId, Integer nuevosCupos) {
		CupoDisponible cupo = cupoRepository.findById(cupoId)
				.orElseThrow(() -> new RecursoNoEncontradoException(
						"Cupo no encontrado: " + cupoId
				));

		cupo.setCuposDisponibles(nuevosCupos);
		return toCupoResponse(cupoRepository.save(cupo));
	}

	@Override
	public void decrementarCupo(Long boxId, LocalDate fecha) {
		CupoDisponible cupo = cupoRepository.findByBoxIdAndFecha(boxId, fecha)
				.orElseThrow(() -> new RecursoNoEncontradoException(
						"No hay cupo para ese box y fecha"
				));

		if (!cupo.tieneCupoDisponible()) {
			throw new ReglaNegocioException(
					"Sin cupos disponibles para box " + boxId + " en " + fecha
			);
		}

		cupo.decrementarCupo();
		cupoRepository.save(cupo);
	}

	// === PRIVADOS ===

	private Servicio buscarServicio(Long id) {
		return servicioRepository.findById(id)
				.orElseThrow(() -> new RecursoNoEncontradoException(
						"Servicio no encontrado: " + id
				));
	}

	private Box buscarBox(Long id) {
		return boxRepository.findById(id)
				.orElseThrow(() -> new RecursoNoEncontradoException(
						"Box no encontrado: " + id
				));
	}

	private void inicializarCuposParaBox(Box box) {
		LocalDate hoy = LocalDate.now(clock);
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
