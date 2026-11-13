package pe.edu.upeu.usuario.controller;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import pe.edu.upeu.usuario.service.DocenteSeguimientoService;
@RestController @RequestMapping("/api/v1/usuarios/docente")
public class DocenteSeguimientoController {
 private final DocenteSeguimientoService service;
 public DocenteSeguimientoController(DocenteSeguimientoService service){this.service=service;}
 @GetMapping("/seguimiento") public DocenteSeguimientoService.Seguimiento consultar(@AuthenticationPrincipal Jwt jwt){return service.consultar(jwt.getSubject(),"Bearer "+jwt.getTokenValue());}
}
