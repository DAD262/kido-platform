package pe.edu.upeu.pago.config;

import feign.RequestInterceptor;
import java.lang.reflect.Method;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.cloud.openfeign.CircuitBreakerNameResolver;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/** Forwards the caller token so downstream services can apply their own policy. */
@Configuration
public class FeignAuthConfig {
    @Bean
    CircuitBreakerNameResolver circuitBreakerNameResolver() {
        return (clientName, target, method) -> circuitId(clientName, method);
    }

    private String circuitId(String clientName, Method method) {
        String operation = switch (clientName) {
            case "kido-curso-ms" -> "Curso";
            case "kido-inscripcion-ms" -> "Inscripcion";
            case "kido-notificacion-ms" -> "Notificacion";
            default -> "Servicio";
        };
        return "Kido" + operation + Character.toUpperCase(method.getName().charAt(0)) + method.getName().substring(1);
    }

    @Bean
    RequestInterceptor bearerTokenForwarder() {
        return template -> {
            var authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication instanceof JwtAuthenticationToken jwt && jwt.getToken().getTokenValue() != null) {
                template.header("Authorization", "Bearer " + jwt.getToken().getTokenValue());
            }
        };
    }
}
