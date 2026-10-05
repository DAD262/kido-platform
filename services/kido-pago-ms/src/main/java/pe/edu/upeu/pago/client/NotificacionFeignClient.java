package pe.edu.upeu.pago.client;

import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "kido-notificacion-ms", fallbackFactory = NotificacionFeignFallbackFactory.class)
public interface NotificacionFeignClient {
    @PostMapping("/internal/v1/notificaciones")
    void enviar(@RequestBody Map<String, Object> request);
}
