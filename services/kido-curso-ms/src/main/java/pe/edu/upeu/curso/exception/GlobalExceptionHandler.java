package pe.edu.upeu.curso.exception;

import java.time.LocalDateTime;
import java.util.*;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<?> notFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(404).body(error(404, ex.getMessage()));
    }
    @ExceptionHandler({IllegalArgumentException.class})
    ResponseEntity<?> badRequest(RuntimeException ex) {
        return ResponseEntity.badRequest().body(error(400, ex.getMessage()));
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<?> validation(MethodArgumentNotValidException ex) {
        var fields = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage()).toList();
        return ResponseEntity.badRequest().body(Map.of("status", 400, "message", "Datos inválidos", "fields", fields));
    }
    private Map<String,Object> error(int status, String message) {
        return Map.of("timestamp", LocalDateTime.now(), "status", status, "message", message);
    }
}
