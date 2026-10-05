package pe.edu.upeu.pago.client;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pe.edu.upeu.pago.dto.InscripcionDto;

/** Keeps the service API stable while routing the internal call through Feign. */
@Component
@RequiredArgsConstructor
public class InscripcionClient {
    private final InscripcionFeignClient feign;

    public InscripcionDto crearCompra(Long estudianteId, Long cursoId, List<Long> leccionIds) {
        return feign.crearCompra(new InscripcionFeignClient.CrearCompraRequest(estudianteId, cursoId, leccionIds));
    }

    public InscripcionDto buscar(Long estudianteId, Long cursoId) {
        return feign.buscar(estudianteId, cursoId);
    }

    public InscripcionDto revocar(Long estudianteId, Long cursoId) {
        return feign.revocar(estudianteId, cursoId);
    }
}
