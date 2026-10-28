package pe.edu.upeu.usuario.controller;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.upeu.usuario.dto.DeseadoResponse;
import pe.edu.upeu.usuario.service.DeseadosService;

@RestController
@RequestMapping("/api/v1/usuarios/me/deseados")
public class DeseadosController {
    private final DeseadosService servicio;

    public DeseadosController(DeseadosService servicio) { this.servicio = servicio; }

    @GetMapping
    public List<DeseadoResponse> listar(@AuthenticationPrincipal Jwt jwt) {
        return servicio.listar(jwt.getSubject());
    }

    /** PUT es idempotente, a diferencia de un POST que puede duplicarse por reintentos. */
    @PutMapping("/{cursoId}")
    public DeseadoResponse guardar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long cursoId) {
        return servicio.guardar(jwt.getSubject(), cursoId);
    }

    @DeleteMapping("/{cursoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void quitar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long cursoId) {
        servicio.quitar(jwt.getSubject(), cursoId);
    }
}
