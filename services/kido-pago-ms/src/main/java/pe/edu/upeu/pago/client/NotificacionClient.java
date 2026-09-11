package pe.edu.upeu.pago.client;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class NotificacionClient {
    private static final Logger log = LoggerFactory.getLogger(NotificacionClient.class);
    private final RestClient.Builder builder;
    public NotificacionClient(@Qualifier("loadBalancedRestClientBuilder") RestClient.Builder builder) { this.builder = builder; }
    public void enviar(Long usuarioId, String tipo, String asunto, String mensaje) {
        try {
            builder.build().post().uri("http://kido-notificacion-ms/internal/v1/notificaciones")
                .body(Map.of("usuarioId", usuarioId, "tipo", tipo, "canal", "IN_APP", "asunto", asunto, "mensaje", mensaje))
                .retrieve().toBodilessEntity();
        } catch (Exception e) {
            log.warn("Notificación diferida: {}", e.getMessage());
        }
    }
}
