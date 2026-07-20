package io.konvex.controller;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
		return ResponseEntity
				.status(HttpStatus.BAD_REQUEST)
				.body(Map.of(
						"error", "Bad Request",
						"message", ex.getMessage()));
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<Map<String, String>> handleUnreadableBody(HttpMessageNotReadableException ex) {
		Throwable cause = ex;
		while (cause != null) {
			if (cause instanceof IllegalArgumentException illegalArgumentException) {
				return handleIllegalArgument(illegalArgumentException);
			}
			cause = cause.getCause();
		}

		return ResponseEntity
				.status(HttpStatus.BAD_REQUEST)
				.body(Map.of(
						"error", "Bad Request",
						"message", "Request body is malformed or has invalid field types"));
	}
}
