package pe.edu.upeu.usuario.exception;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

/**
 * Errores con mensaje en español y, cuando aplica, el mapa {@code campos} (campo → problema)
 * para que el formulario marque exactamente qué corregir.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(RecursoNoEncontradoException.class)
    ResponseEntity<Map<String, Object>> noEncontrado(RecursoNoEncontradoException e) {
        return cuerpo(HttpStatus.NOT_FOUND, e.getMessage(), null);
    }

    @ExceptionHandler(ConflictoException.class)
    ResponseEntity<Map<String, Object>> conflicto(ConflictoException e) {
        return cuerpo(HttpStatus.CONFLICT, e.getMessage(), null);
    }

    /** Carrera entre dos registros con el mismo correo o DNI: lo frena la restricción única. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<Map<String, Object>> duplicado(DataIntegrityViolationException e) {
        log.info("Restricción de unicidad: {}", e.getMostSpecificCause().getMessage());
        return cuerpo(HttpStatus.CONFLICT, "Ese correo o DNI ya está registrado.", null);
    }

    @ExceptionHandler(DemasiadosIntentosException.class)
    ResponseEntity<Map<String, Object>> demasiados(DemasiadosIntentosException e) {
        return cuerpo(HttpStatus.TOO_MANY_REQUESTS, e.getMessage(), null);
    }

    @ExceptionHandler(IdentidadNoDisponibleException.class)
    ResponseEntity<Map<String, Object>> identidad(IdentidadNoDisponibleException e) {
        log.warn("Keycloak Admin API: {}", e.getMessage(), e.getCause());
        return cuerpo(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage(), null);
    }

    /** Los validadores lanzan "campo: mensaje"; se separa para resaltar el campo en el formulario. */
    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<Map<String, Object>> invalido(IllegalArgumentException e) {
        String mensaje = e.getMessage() == null ? "Solicitud inválida." : e.getMessage();
        int separador = mensaje.indexOf(": ");
        if (separador > 0 && mensaje.substring(0, separador).matches("[a-zA-Z]+")) {
            String campo = mensaje.substring(0, separador);
            String detalle = mensaje.substring(separador + 2);
            return cuerpo(HttpStatus.BAD_REQUEST, detalle, Map.of(campo, detalle));
        }
        return cuerpo(HttpStatus.BAD_REQUEST, mensaje, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String, Object>> validacion(MethodArgumentNotValidException e) {
        Map<String, String> campos = new LinkedHashMap<>();
        for (FieldError error : e.getBindingResult().getFieldErrors()) {
            String campo = error.getField().replaceFirst("^docente\\.", "");
            campos.putIfAbsent(campo, error.getDefaultMessage());
        }
        String primero = campos.values().stream().findFirst().orElse("Revisa los datos del formulario.");
        return cuerpo(HttpStatus.BAD_REQUEST, primero, campos);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<Map<String, Object>> archivoGrande(MaxUploadSizeExceededException e) {
        String mensaje = "El PDF puede pesar como máximo 5 MB.";
        return cuerpo(HttpStatus.PAYLOAD_TOO_LARGE, mensaje, Map.of("documento", mensaje));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MissingServletRequestPartException.class})
    ResponseEntity<Map<String, Object>> ilegible(Exception e) {
        return cuerpo(HttpStatus.BAD_REQUEST, "El formulario llegó incompleto o con un formato inesperado.", null);
    }

    private ResponseEntity<Map<String, Object>> cuerpo(HttpStatus estado, String mensaje, Map<String, String> campos) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("timestamp", OffsetDateTime.now().toString());
        cuerpo.put("status", estado.value());
        cuerpo.put("error", estado.getReasonPhrase());
        cuerpo.put("message", mensaje);
        if (campos != null && !campos.isEmpty()) {
            cuerpo.put("campos", campos);
        }
        return ResponseEntity.status(estado).body(cuerpo);
    }
}
