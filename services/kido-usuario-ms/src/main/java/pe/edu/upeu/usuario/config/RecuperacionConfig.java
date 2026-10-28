package pe.edu.upeu.usuario.config;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
@Configuration
public class RecuperacionConfig {
    @Bean public Clock relojRecuperacion() { return Clock.systemUTC(); }
}
