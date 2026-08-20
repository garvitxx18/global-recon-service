package global.recon.service.controller;

import global.recon.service.service.InvalidRequestException;
import global.recon.service.service.LlmDiscoveryException;
import global.recon.service.service.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class RestExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, String>> notFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error("NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<Map<String, String>> badRequest(InvalidRequestException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error("INVALID_REQUEST", ex.getMessage()));
    }

    @ExceptionHandler(LlmDiscoveryException.class)
    public ResponseEntity<Map<String, String>> discoveryFailed(LlmDiscoveryException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(error("DISCOVERY_FAILED", ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> unexpected(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(error("INTERNAL_ERROR", ex.getMessage() == null ? "Unexpected error" : ex.getMessage()));
    }

    private Map<String, String> error(String code, String message) {
        return Map.of("error", code, "message", message == null ? "" : message);
    }
}
