package pe.edu.upeu.pago.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    @Primary
    RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }

    @Bean
    @LoadBalanced
    @Qualifier("loadBalancedRestClientBuilder")
    RestClient.Builder loadBalancedRestClientBuilder() {
        return RestClient.builder();
    }

    @Bean
    RestClient mercadoPagoRestClient() {
        var http = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        http.setConnectTimeout(java.time.Duration.ofSeconds(5));
        http.setReadTimeout(java.time.Duration.ofSeconds(20));
        return RestClient.builder()
                .requestFactory(http)
                .baseUrl("https://api.mercadopago.com")
                .build();
    }
}
