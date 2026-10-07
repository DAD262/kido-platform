package pe.edu.upeu.pago.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import pe.edu.upeu.pago.dto.InscripcionDto;
import pe.edu.upeu.pago.exception.DependenciaNoDisponibleException;

@Component
public class InscripcionFeignFallbackFactory implements FallbackFactory<InscripcionFeignClient> {
    private static final Logger log = LoggerFactory.getLogger(InscripcionFeignFallbackFactory.class);

    @Override
    public InscripcionFeignClient create(Throwable cause) {
        return new InscripcionFeignClient() {
            private DependenciaNoDisponibleException unavailable() {
                log.warn("Circuit Breaker de kido-inscripcion-ms activado: {}", cause.toString());
                return new DependenciaNoDisponibleException("kido-inscripcion-ms", cause);
            }

            @Override
            public InscripcionDto crearCompra(CrearCompraRequest request) { throw unavailable(); }

            @Override
            public InscripcionDto buscar(Long estudianteId, Long cursoId) { throw unavailable(); }

            @Override
            public InscripcionDto revocar(Long estudianteId, Long cursoId) { throw unavailable(); }
            @Override
            public pe.edu.upeu.pago.dto.CertificadoInscripcionDto certificado(Long id) { throw unavailable(); }
            @Override
            public pe.edu.upeu.pago.dto.CertificadoInscripcionDto confirmarPagoCertificado(Long id) { throw unavailable(); }
        };
    }
}
