package pe.edu.upeu.usuario.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class CatalogoHttpConfig {
    @Bean
    RestClient.Builder catalogoRestClientBuilder() {
        return RestClient.builder();
    }
}
