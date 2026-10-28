package pe.edu.upeu.inscripcion.exception;

import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class GlobalExceptionHandler {
 @ExceptionHandler(ResourceNotFoundException.class)
 ResponseEntity<?> notFound(ResourceNotFoundException e){return ResponseEntity.status(404).body(Map.of("status",404,"message",e.getMessage()));}
 @ExceptionHandler({IllegalArgumentException.class,DataIntegrityViolationException.class})
 ResponseEntity<?> bad(Exception e){return ResponseEntity.badRequest().body(Map.of("status",400,"message",e instanceof DataIntegrityViolationException?"La inscripción ya existe":e.getMessage()));}
 @ExceptionHandler(org.springframework.web.client.RestClientException.class)
 ResponseEntity<?> externo(org.springframework.web.client.RestClientException e){org.slf4j.LoggerFactory.getLogger(getClass()).warn("Consulta de certificado no disponible [{} / {}]",e.getClass().getSimpleName(),e.getMostSpecificCause().getClass().getSimpleName());return ResponseEntity.status(502).body(Map.of("status",502,"message","No pudimos consultar los datos del curso o la cuenta. Inténtalo de nuevo."));}
 @ExceptionHandler(IllegalStateException.class)
 ResponseEntity<?> conflict(IllegalStateException e){return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("status",409,"message",e.getMessage()));}
 @ExceptionHandler(MethodArgumentNotValidException.class)
 ResponseEntity<?> validation(MethodArgumentNotValidException e){return ResponseEntity.badRequest().body(Map.of("status",400,"message","Datos inválidos","fields",e.getBindingResult().getFieldErrors().stream().map(f->f.getField()+": "+f.getDefaultMessage()).toList()));}
}
