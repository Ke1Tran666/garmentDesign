package com.garmentDesign.exception;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<Map<String, Object>> handleInvalidBody(HttpMessageNotReadableException exception,
			HttpServletRequest request) {
		return buildResponse(HttpStatus.BAD_REQUEST, "Dữ liệu gửi lên không hợp lệ", request);
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<Map<String, Object>> handleDataConflict(DataIntegrityViolationException exception,
			HttpServletRequest request) {
		return buildResponse(HttpStatus.CONFLICT, "Dữ liệu đã tồn tại hoặc bị trùng lặp", request);
	}

	/*
	 * Hiện tại các service đang sử dụng RuntimeException cho lỗi nghiệp vụ.
	 */
	@ExceptionHandler(RuntimeException.class)
	public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException exception,
			HttpServletRequest request) {
		String message = exception.getMessage();

		if (message == null || message.isBlank()) {
			message = "Không thể thực hiện yêu cầu";
		}

		return buildResponse(HttpStatus.BAD_REQUEST, message, request);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<Map<String, Object>> handleUnexpectedException(Exception exception,
			HttpServletRequest request) {
		/*
		 * Không trả exception.getMessage() cho lỗi hệ thống, tránh lộ thông tin nội bộ.
		 */
		return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Hệ thống đang gặp sự cố. Vui lòng thử lại sau",
				request);
	}

	private ResponseEntity<Map<String, Object>> buildResponse(HttpStatus status, String message,
			HttpServletRequest request) {
		Map<String, Object> response = new LinkedHashMap<>();

		response.put("timestamp", LocalDateTime.now());
		response.put("status", status.value());
		response.put("error", status.getReasonPhrase());
		response.put("message", message);
		response.put("path", request.getRequestURI());

		return ResponseEntity.status(status).body(response);
	}
}