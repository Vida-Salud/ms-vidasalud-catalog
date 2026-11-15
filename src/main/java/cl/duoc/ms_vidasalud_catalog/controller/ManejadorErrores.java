package cl.duoc.ms_vidasalud_catalog.controller;

import cl.duoc.ms_vidasalud_catalog.exception.DatoInvalidoException;
import cl.duoc.ms_vidasalud_catalog.exception.RecursoNoEncontradoException;
import cl.duoc.ms_vidasalud_catalog.exception.ReglaNegocioException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.exc.InvalidFormatException;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Un solo formato de error (ProblemDetail, RFC 9457) para toda la API.
 * <p>
 * {@code @Order(HIGHEST_PRECEDENCE)}: el handler de Spring Boot (problemdetails.enabled) se registra
 * con orden 0 y, sin esto, respondería primero a @Valid sin agregar "errores".
 * No hay un {@code @ExceptionHandler(Exception.class)}: atraparía el AccessDeniedException de
 * {@code @PreAuthorize} y un 403 saldría como 500.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class ManejadorErrores {

	@ExceptionHandler(RecursoNoEncontradoException.class)
	public ProblemDetail noEncontrado(RecursoNoEncontradoException e) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
	}

	@ExceptionHandler(ReglaNegocioException.class)
	public ProblemDetail reglaDeNegocio(ReglaNegocioException e) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
	}

	@ExceptionHandler(DatoInvalidoException.class)
	public ProblemDetail datoInvalido(DatoInvalidoException e) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ProblemDetail manejarValidacion(MethodArgumentNotValidException e) {
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(
				HttpStatus.BAD_REQUEST, "La petición tiene campos inválidos");
		Map<String, String> errores = new LinkedHashMap<>();
		e.getBindingResult().getFieldErrors()
				.forEach(error -> errores.putIfAbsent(error.getField(), error.getDefaultMessage()));
		problema.setProperty("errores", errores);
		return problema;
	}

	/**
	 * Un valor de enum desconocido (por ejemplo "tipo": "XYZ") se reporta como error del campo,
	 * con los valores permitidos. Cualquier otro problema de lectura es JSON inválido.
	 */
	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ProblemDetail manejarCuerpoIlegible(HttpMessageNotReadableException e) {
		if (e.getCause() instanceof InvalidFormatException invalido
				&& invalido.getTargetType() != null
				&& invalido.getTargetType().isEnum()
				&& !invalido.getPath().isEmpty()) {

			ProblemDetail problema = ProblemDetail.forStatusAndDetail(
					HttpStatus.BAD_REQUEST, "La petición tiene campos inválidos");
			problema.setProperty("errores", Map.of(
					nombreCampo(invalido.getPath()),
					"Valor inválido. Valores permitidos: " + valoresPermitidos(invalido.getTargetType())));
			return problema;
		}

		return ProblemDetail.forStatusAndDetail(
				HttpStatus.BAD_REQUEST, "El cuerpo de la petición no es un JSON válido");
	}

	private String nombreCampo(List<JacksonException.Reference> ruta) {
		return ruta.get(ruta.size() - 1).getPropertyName();
	}

	private String valoresPermitidos(Class<?> tipoEnum) {
		return Arrays.stream(tipoEnum.getEnumConstants())
				.map(Object::toString)
				.collect(Collectors.joining(", "));
	}
}
