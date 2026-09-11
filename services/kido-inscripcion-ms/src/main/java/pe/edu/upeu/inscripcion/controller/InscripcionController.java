package pe.edu.upeu.inscripcion.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import pe.edu.upeu.inscripcion.dto.*;
import pe.edu.upeu.inscripcion.service.InscripcionService;
import java.util.List;

@RestController @RequestMapping("/api/v1/inscripciones") @RequiredArgsConstructor
public class InscripcionController {
 private final InscripcionService service;
 @GetMapping public List<InscripcionResponse> listar(){return service.listar();}
 @GetMapping("/{id}") public InscripcionResponse obtener(@PathVariable Long id){return service.obtener(id);}
 @PostMapping public ResponseEntity<InscripcionResponse> crear(@Valid @RequestBody InscripcionRequest r){return ResponseEntity.status(201).body(service.crear(r));}
 @PutMapping("/{id}/estado") public InscripcionResponse estado(@PathVariable Long id,@Valid @RequestBody EstadoRequest r){return service.cambiarEstado(id,r);}
 @PutMapping("/{id}/lecciones/{leccionId}/completar") public InscripcionResponse completar(@PathVariable Long id,@PathVariable Long leccionId){return service.completarLeccion(id,leccionId);}
 @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void eliminar(@PathVariable Long id){service.eliminar(id);}
}
