package pe.edu.upeu.pago.client;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
public class NotificacionFeignFallbackFactory implements FallbackFactory<NotificacionFeignClient> {
    private static final Logger log = LoggerFactory.getLogger(NotificacionFeignFallbackFactory.class);

    @Override
    public NotificacionFeignClient create(Throwable cause) {
        return request -> log.warn("Notificación diferida: {}", cause.toString());
    }
}
