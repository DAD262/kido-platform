package pe.edu.upeu.pago.client;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import pe.edu.upeu.pago.dto.InscripcionDto;

@Component
public class InscripcionClient {
    private final RestClient.Builder builder;
    public InscripcionClient(@Qualifier("loadBalancedRestClientBuilder") RestClient.Builder builder) { this.builder = builder; }
    public InscripcionDto crearCompra(Long estudianteId, Long cursoId, List<Long> lecciones) {
        return builder.build().post().uri("http://kido-inscripcion-ms/internal/v1/inscripciones/compra")
            .body(Map.of("estudianteId", estudianteId, "cursoId", cursoId, "leccionIds", lecciones)).retrieve().body(InscripcionDto.class);
    }
    public InscripcionDto buscar(Long e, Long c) {
        return builder.build().get().uri("http://kido-inscripcion-ms/internal/v1/inscripciones/por-estudiante-curso?estudianteId={e}&cursoId={c}", e, c).retrieve().body(InscripcionDto.class);
    }
    public InscripcionDto revocar(Long e, Long c) {
        return builder.build().put().uri("http://kido-inscripcion-ms/internal/v1/inscripciones/por-estudiante-curso/revocar?estudianteId={e}&cursoId={c}", e, c).retrieve().body(InscripcionDto.class);
    }
}
