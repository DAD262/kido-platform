package pe.edu.upeu.pago.client;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import pe.edu.upeu.pago.dto.CursoCompraDto;

@Component
public class CursoClient {
    private final RestClient.Builder builder;
    public CursoClient(@Qualifier("loadBalancedRestClientBuilder") RestClient.Builder builder) { this.builder = builder; }
    public CursoCompraDto obtener(Long id) {
        return builder.build().get().uri("http://kido-curso-ms/internal/v1/cursos/{id}/resumen-compra", id).retrieve().body(CursoCompraDto.class);
    }
}
