package pe.edu.upeu.curso.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import pe.edu.upeu.curso.dto.*;
import pe.edu.upeu.curso.service.CursoService;
import java.util.List;

@RestController
@RequestMapping("/api/v1/cursos")
@RequiredArgsConstructor
public class CursoController {
    private final CursoService service;
    @GetMapping public List<CursoResponse> listar() { return service.listar(); }
    @GetMapping("/publicados") public List<CursoResponse> publicados(){ return service.publicados(); }
    @GetMapping("/docente/{docenteId}") public List<CursoResponse> porDocente(@PathVariable Long docenteId){ return service.porDocente(docenteId); }
    @GetMapping("/{id}") public CursoResponse obtener(@PathVariable Long id) { return service.obtener(id); }
    @PostMapping public ResponseEntity<CursoResponse> crear(@Valid @RequestBody CursoRequest r) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(r));
    }
    @PutMapping("/{id}") public CursoResponse actualizar(@PathVariable Long id, @Valid @RequestBody CursoRequest r) {
        return service.actualizar(id, r);
    }
    @PostMapping("/{id}/enviar-revision") public CursoResponse enviarRevision(@PathVariable Long id){ return service.enviarRevision(id); }
    @PostMapping("/{id}/aprobar") public CursoResponse aprobar(@PathVariable Long id){ return service.aprobar(id); }
    @PostMapping("/{id}/rechazar") public CursoResponse rechazar(@PathVariable Long id,@Valid @RequestBody RechazoCursoRequest r){ return service.rechazar(id,r.motivo()); }
    @PostMapping("/{id}/archivar") public CursoResponse archivar(@PathVariable Long id){ return service.archivar(id); }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) { service.eliminar(id); }
}
