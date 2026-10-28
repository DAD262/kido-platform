package pe.edu.upeu.usuario.controller;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import pe.edu.upeu.usuario.dto.*;
import pe.edu.upeu.usuario.service.AdminCuentaService;
@RestController
@RequestMapping("/api/v1/usuarios/admin/cuentas")
public class AdminCuentaController {
    private final AdminCuentaService service;
    private final pe.edu.upeu.usuario.service.SupervisionService supervision;
    public AdminCuentaController(AdminCuentaService service, pe.edu.upeu.usuario.service.SupervisionService supervision) { this.service=service; this.supervision=supervision; }
    @GetMapping("/{id}/supervision") public java.util.List<Long> supervision(@PathVariable String id){return supervision.ids(id);}
    @PutMapping("/{id}/supervision") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void asignar(@AuthenticationPrincipal Jwt jwt,@PathVariable String id,@Valid @RequestBody SupervisionRequest r){supervision.asignar(jwt.getSubject(),id,r);}
    @GetMapping public List<CuentaAdminResponse> listar() { return service.listar(); }
    @PutMapping("/{id}") public CuentaAdminResponse actualizar(@AuthenticationPrincipal Jwt jwt,
        @PathVariable String id,@Valid @RequestBody ActualizarCuentaRequest r) { return service.actualizar(jwt.getSubject(),id,r); }
    @PostMapping("/{id}/password") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void password(@AuthenticationPrincipal Jwt jwt,@PathVariable String id,
        @Valid @RequestBody RestablecerPasswordRequest r) { service.restablecer(jwt.getSubject(),id,r); }
}
