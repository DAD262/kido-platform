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
    @GetMapping("/{id}") public CursoResponse obtener(@PathVariable Long id) { return service.obtener(id); }
    @PostMapping public ResponseEntity<CursoResponse> crear(@Valid @RequestBody CursoRequest r) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(r));
    }
    @PutMapping("/{id}") public CursoResponse actualizar(@PathVariable Long id, @Valid @RequestBody CursoRequest r) {
        return service.actualizar(id, r);
    }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) { service.eliminar(id); }
}
