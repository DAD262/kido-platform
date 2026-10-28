package pe.edu.upeu.usuario.controller;
import java.util.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import pe.edu.upeu.usuario.service.SupervisionService;
@RestController @RequestMapping("/api/v1/usuarios/supervision")
public class SupervisionController {
    private final SupervisionService service;
    public SupervisionController(SupervisionService service){this.service=service;}
    @GetMapping("/cursos") public List<Map<String,Object>> cursos(@AuthenticationPrincipal Jwt jwt){return service.listar(jwt.getSubject());}
    @GetMapping("/cursos/{id}") public Map<String,Object> seguimiento(@AuthenticationPrincipal Jwt jwt,@PathVariable Long id){return service.seguimiento(jwt.getSubject(),id);}
}
