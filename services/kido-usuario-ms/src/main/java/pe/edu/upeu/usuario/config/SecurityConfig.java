package pe.edu.upeu.usuario.config;

import java.time.Clock;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import pe.edu.upeu.usuario.service.PerfilService;
import pe.edu.upeu.usuario.support.LimitadorIntentos;

/** Mismas reglas que el Gateway, para que una llamada directa al servicio no las salte. */
@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/info", "/actuator/prometheus", "/error").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/usuarios/docentes-publicos").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/usuarios/registro").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/usuarios/recuperacion/solicitar", "/api/v1/usuarios/recuperacion/confirmar").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/usuarios/registro/disponibilidad", "/api/v1/usuarios/recuperacion/configuracion").permitAll()
                        .requestMatchers("/api/v1/usuarios/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/v1/usuarios/supervision/**").hasAnyRole("SUPERVISOR", "ADMIN")
                        .requestMatchers("/api/v1/usuarios/me/deseados", "/api/v1/usuarios/me/deseados/**").hasRole("ESTUDIANTE")
                        .requestMatchers("/api/v1/usuarios/me/**", "/api/v1/usuarios/me").authenticated()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(o -> o.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));
        return http.build();
    }

    @Bean
    Converter<Jwt, ? extends AbstractAuthenticationToken> jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> PerfilService.roles(jwt).stream()
                .map(rol -> new SimpleGrantedAuthority("ROLE_" + rol))
                .map(GrantedAuthority.class::cast)
                .toList());
        return converter;
    }

    /** 5 registros cada 15 minutos por IP. */
    @Bean
    LimitadorIntentos limitadorRegistro() {
        return new LimitadorIntentos(5, Duration.ofMinutes(15), Clock.systemUTC());
    }

    /** 40 consultas de disponibilidad por minuto por IP. */
    @Bean
    LimitadorIntentos limitadorConsultas() {
        return new LimitadorIntentos(40, Duration.ofMinutes(1), Clock.systemUTC());
    }
}
