package pe.edu.upeu.pago.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

/** Hold unprocessed Saga events for re-delivery; alert operators on recurring errors. */
@Configuration
public class SagaKafkaErrorConfig {
    @Bean
    public CommonErrorHandler sagaKafkaErrorHandler() {
        return new DefaultErrorHandler(new FixedBackOff(3000L, FixedBackOff.UNLIMITED_ATTEMPTS));
    }
}
