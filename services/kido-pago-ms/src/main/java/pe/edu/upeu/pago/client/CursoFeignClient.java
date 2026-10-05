package pe.edu.upeu.pago.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import pe.edu.upeu.pago.dto.CursoCompraDto;

@FeignClient(name = "kido-curso-ms", fallbackFactory = CursoFeignFallbackFactory.class)
public interface CursoFeignClient {
    @GetMapping("/internal/v1/cursos/{id}/resumen-compra")
    CursoCompraDto obtener(@PathVariable("id") Long id);
}
