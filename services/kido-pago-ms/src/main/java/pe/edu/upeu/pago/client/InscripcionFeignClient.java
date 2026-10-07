package pe.edu.upeu.pago.client;

import java.util.List;
import pe.edu.upeu.pago.dto.InscripcionDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "kido-inscripcion-ms", fallbackFactory = InscripcionFeignFallbackFactory.class)
public interface InscripcionFeignClient {
    @PostMapping("/internal/v1/inscripciones/compra")
    InscripcionDto crearCompra(@RequestBody CrearCompraRequest request);

    @GetMapping("/internal/v1/inscripciones/por-estudiante-curso")
    InscripcionDto buscar(@RequestParam("estudianteId") Long estudianteId,
                          @RequestParam("cursoId") Long cursoId);

    @PutMapping("/internal/v1/inscripciones/por-estudiante-curso/revocar")
    InscripcionDto revocar(@RequestParam("estudianteId") Long estudianteId,
                           @RequestParam("cursoId") Long cursoId);

    record CrearCompraRequest(Long estudianteId, Long cursoId, List<Long> leccionIds) {}
}
