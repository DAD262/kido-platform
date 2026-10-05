package pe.edu.upeu.pago.client;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificacionClient {
    private final NotificacionFeignClient feign;

    public void enviar(Long usuarioId, String tipo, String asunto, String mensaje) {
        feign.enviar(Map.of("usuarioId", usuarioId, "tipo", tipo, "canal", "IN_APP", "asunto", asunto, "mensaje", mensaje));
    }
}
