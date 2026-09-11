package pe.edu.upeu.pago.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.*;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {
 @Bean @LoadBalanced
 RestClient.Builder loadBalancedRestClientBuilder(){ return RestClient.builder(); }

 @Bean
 RestClient mercadoPagoRestClient(){
   return RestClient.builder().baseUrl("https://api.mercadopago.com").build();
 }
}
