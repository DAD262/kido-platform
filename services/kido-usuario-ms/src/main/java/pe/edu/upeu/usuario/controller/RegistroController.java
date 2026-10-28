package pe.edu.upeu.usuario.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Qualifier;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import pe.edu.upeu.usuario.dto.RegistroRequest;
import pe.edu.upeu.usuario.dto.RegistroResponse;
import pe.edu.upeu.usuario.exception.DemasiadosIntentosException;
import pe.edu.upeu.usuario.service.RegistroService;
import pe.edu.upeu.usuario.support.LimitadorIntentos;

/** Endpoints públicos (sin token): crear cuenta y comprobar si un correo está libre. */
@RestController
@RequestMapping("/api/v1/usuarios/registro")
public class RegistroController {

    private final RegistroService registro;
    private final LimitadorIntentos limitadorRegistro;
    private final LimitadorIntentos limitadorConsultas;

    public RegistroController(RegistroService registro,
            @Qualifier("limitadorRegistro") LimitadorIntentos limitadorRegistro,
            @Qualifier("limitadorConsultas") LimitadorIntentos limitadorConsultas) {
        this.registro = registro;
        this.limitadorRegistro = limitadorRegistro;
        this.limitadorConsultas = limitadorConsultas;
    }

    /**
     * multipart/form-data con la parte {@code datos} (JSON de {@link RegistroRequest}) y, para
     * docentes, la parte {@code documento} (PDF de hasta 5 MB).
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public RegistroResponse registrar(@Valid @RequestPart("datos") RegistroRequest datos,
            @RequestPart(value = "documento", required = false) MultipartFile documento,
            HttpServletRequest request) {
        if (!limitadorRegistro.permitir(ipCliente(request))) {
            throw new DemasiadosIntentosException("Demasiados registros desde esta red. Espera unos minutos e inténtalo de nuevo.");
        }
        return registro.registrar(datos, documento);
    }

    @GetMapping("/disponibilidad")
    public Map<String, Boolean> disponibilidad(@RequestParam String email, HttpServletRequest request) {
        if (!limitadorConsultas.permitir(ipCliente(request))) {
            throw new DemasiadosIntentosException("Demasiadas consultas. Espera un momento.");
        }
        return Map.of("disponible", registro.emailDisponible(email));
    }

    /** El Gateway agrega X-Forwarded-For; sin Gateway se usa la IP directa. */
    static String ipCliente(HttpServletRequest request) {
        String reenviada = request.getHeader("X-Forwarded-For");
        if (reenviada != null && !reenviada.isBlank()) {
            return reenviada.split(",")[0].strip();
        }
        return request.getRemoteAddr();
    }
}
