package pe.edu.upeu.pago.client;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pe.edu.upeu.pago.dto.CursoCompraDto;

/** Keeps existing service call sites while using the Feign circuit breaker. */
@Component
@RequiredArgsConstructor
public class CursoClient {
    private final CursoFeignClient feign;

    public CursoCompraDto obtener(Long id) {
        return feign.obtener(id);
    }
}
