package pe.edu.upeu.pago.exception;

import java.time.OffsetDateTime;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<?> notFound(ResourceNotFoundException e) { return body(HttpStatus.NOT_FOUND, e.getMessage()); }

    @ExceptionHandler(DependenciaNoDisponibleException.class)
    ResponseEntity<?> dependencyUnavailable(DependenciaNoDisponibleException e) {
        return body(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage());
    }

    @ExceptionHandler(org.springframework.web.client.RestClientException.class)
    ResponseEntity<?> paymentUnavailable(org.springframework.web.client.RestClientException e) {
        return body(HttpStatus.BAD_GATEWAY, "No pudimos comunicarnos con el proveedor de pagos. Tu compra no se ha confirmado; vuelve a intentarlo.");
    }

    @ExceptionHandler({org.springframework.dao.DataIntegrityViolationException.class,org.springframework.orm.ObjectOptimisticLockingFailureException.class})
    ResponseEntity<?> concurrentChange(RuntimeException e){return body(HttpStatus.CONFLICT,"La operación ya se registró o el saldo cambió. Recarga antes de continuar.");}

    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    ResponseEntity<?> archivoGrande(org.springframework.web.multipart.MaxUploadSizeExceededException e){return body(HttpStatus.PAYLOAD_TOO_LARGE,"La constancia no debe superar 5 MB");}

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<?> badRequest(IllegalArgumentException e) { return body(HttpStatus.BAD_REQUEST, e.getMessage()); }

    @ExceptionHandler(IllegalStateException.class)
    ResponseEntity<?> conflict(IllegalStateException e) { return body(HttpStatus.CONFLICT, e.getMessage()); }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<?> validation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage()).toList().toString();
        return body(HttpStatus.BAD_REQUEST, message);
    }

    private ResponseEntity<?> body(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of(
                "timestamp", OffsetDateTime.now().toString(),
                "status", status.value(),
                "error", status.getReasonPhrase(),
                "message", message));
    }
}
