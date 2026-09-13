package cl.duoc.ms_vidasalud_catalog.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.stream.Collectors;

@RestControllerAdvice
public class ManejadorErrores {

	@ExceptionHandler(ResponseStatusException.class)
	public ProblemDetail manejarResponseStatusException(ResponseStatusException ex) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(ex.getStatusCode(), ex.getReason());
		problem.setType(URI.create("about:blank"));
		problem.setTitle(HttpStatus.valueOf(ex.getStatusCode().value()).getReasonPhrase());
		return problem;
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ProblemDetail manejarValidacion(MethodArgumentNotValidException ex) {
		String errores = ex.getBindingResult()
				.getFieldErrors()
				.stream()
				.map(e -> e.getField() + ": " + e.getDefaultMessage())
				.collect(Collectors.joining(", "));

		ProblemDetail problem = ProblemDetail.forStatusAndDetail(
				HttpStatus.BAD_REQUEST,
				"Validación fallida: " + errores
		);
		problem.setType(URI.create("about:blank"));
		problem.setTitle("Bad Request");
		return problem;
	}
}
