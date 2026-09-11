package pe.edu.upeu.inscripcion.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import pe.edu.upeu.inscripcion.dto.*;
import pe.edu.upeu.inscripcion.service.InscripcionService;

@RestController
@RequestMapping("/internal/v1/inscripciones")
@RequiredArgsConstructor
public class InscripcionInternalController {
    private final InscripcionService service;

    @PostMapping("/compra")
    public InscripcionResponse crearPorCompra(@Valid @RequestBody CompraInscripcionRequest request) {
        return service.crearPorCompra(request);
    }

    @GetMapping("/por-estudiante-curso")
    public InscripcionResponse buscarPorEstudianteCurso(
            @RequestParam Long estudianteId, @RequestParam Long cursoId) {
        return service.buscarPorEstudianteCurso(estudianteId, cursoId);
    }

    @PutMapping("/por-estudiante-curso/revocar")
    public InscripcionResponse revocarPorReembolso(
            @RequestParam Long estudianteId, @RequestParam Long cursoId) {
        return service.revocarPorReembolso(estudianteId, cursoId);
    }
}
