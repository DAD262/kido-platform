package pe.edu.upeu.pago.client;

import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import pe.edu.upeu.pago.dto.CursoCompraDto;
import pe.edu.upeu.pago.exception.DependenciaNoDisponibleException;
import pe.edu.upeu.pago.exception.ResourceNotFoundException;

@Component
public class CursoFeignFallbackFactory implements FallbackFactory<CursoFeignClient> {
    private static final Logger log = LoggerFactory.getLogger(CursoFeignFallbackFactory.class);

    @Override
    public CursoFeignClient create(Throwable cause) {
        return id -> {
            if (cause instanceof FeignException.NotFound) {
                throw new ResourceNotFoundException("Curso no encontrado: " + id);
            }
            log.warn("Circuit Breaker de kido-curso-ms activado al consultar curso {}: {}", id, cause.toString());
            throw new DependenciaNoDisponibleException("kido-curso-ms", cause);
        };
    }
}
