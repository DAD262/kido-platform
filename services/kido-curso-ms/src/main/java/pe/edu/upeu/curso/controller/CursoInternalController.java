package pe.edu.upeu.curso.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import pe.edu.upeu.curso.dto.CursoCompraResponse;
import pe.edu.upeu.curso.service.CursoService;

@RestController
@RequestMapping("/internal/v1/cursos")
@RequiredArgsConstructor
public class CursoInternalController {
    private final CursoService service;

    @GetMapping("/{id}/resumen-compra")
    public CursoCompraResponse resumenCompra(@PathVariable Long id) {
        return service.resumenCompra(id);
    }
}
