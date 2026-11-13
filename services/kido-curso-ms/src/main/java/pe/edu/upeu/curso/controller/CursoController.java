package pe.edu.upeu.curso.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;
import pe.edu.upeu.curso.dto.*;
import pe.edu.upeu.curso.service.CursoService;
import pe.edu.upeu.curso.service.CursoPortadaStorageService;
import java.util.List;

@RestController
@RequestMapping("/api/v1/cursos")
@RequiredArgsConstructor
public class CursoController {
    private final CursoService service;
    private final CursoPortadaStorageService portadas;
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
    @PostMapping(value="/{id}/portada", consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public CursoResponse subirPortada(@PathVariable Long id, @RequestPart("archivo") MultipartFile archivo) {
        return portadas.guardar(id, archivo);
    }
    @DeleteMapping("/{id}/portada") public CursoResponse eliminarPortada(@PathVariable Long id) {
        return portadas.eliminar(id);
    }
    @GetMapping("/portadas/{nombre}")
    public ResponseEntity<Resource> verPortada(@PathVariable String nombre) {
        Resource recurso = portadas.cargar(nombre);
        MediaType tipo = nombre.toLowerCase().endsWith(".png") ? MediaType.IMAGE_PNG
                : nombre.toLowerCase().endsWith(".webp") ? MediaType.parseMediaType("image/webp")
                : MediaType.IMAGE_JPEG;
        return ResponseEntity.ok().contentType(tipo).cacheControl(CacheControl.maxAge(java.time.Duration.ofDays(30)).cachePublic()).body(recurso);
    }
    @PostMapping("/{id}/enviar-revision") public CursoResponse enviarRevision(@PathVariable Long id){ return service.enviarRevision(id); }
    @PostMapping("/{id}/aprobar") public CursoResponse aprobar(@PathVariable Long id){ return service.aprobar(id); }
    @PostMapping("/{id}/rechazar") public CursoResponse rechazar(@PathVariable Long id,@Valid @RequestBody RechazoCursoRequest r){ return service.rechazar(id,r.motivo()); }
    @PostMapping("/{id}/archivar") public CursoResponse archivar(@PathVariable Long id){ return service.archivar(id); }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) { service.eliminar(id); }
}
