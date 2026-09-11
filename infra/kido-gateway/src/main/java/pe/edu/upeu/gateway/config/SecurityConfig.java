package pe.edu.upeu.gateway.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

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

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(Customizer.withDefaults())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/actuator/health", "/actuator/info", "/error").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/cursos/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/pagos/webhooks/mercado-pago").permitAll()

                .requestMatchers(HttpMethod.POST, "/api/v1/cursos/**").hasAnyRole("DOCENTE", "ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/v1/cursos/**").hasAnyRole("DOCENTE", "ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/cursos/**").hasAnyRole("DOCENTE", "ADMIN")

                .requestMatchers("/api/v1/inscripciones/**").hasAnyRole("ESTUDIANTE", "ADMIN")

                .requestMatchers(HttpMethod.POST, "/api/v1/pagos/ordenes/*/simular-aprobacion").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/v1/pagos/ordenes/*/reintentar-inscripcion").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/v1/pagos/saldos/liberar").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/v1/pagos/ordenes/docente/**").hasAnyRole("DOCENTE", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/v1/pagos/docentes/**").hasAnyRole("DOCENTE", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/v1/pagos/ordenes/estudiante/**").hasAnyRole("ESTUDIANTE", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/v1/pagos/ordenes/**").hasAnyRole("ESTUDIANTE", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/v1/pagos/ordenes/**").authenticated()

                .requestMatchers(HttpMethod.POST, "/api/v1/retiros/**").hasRole("DOCENTE")
                .requestMatchers(HttpMethod.PUT, "/api/v1/retiros/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/v1/retiros/**").hasAnyRole("DOCENTE", "ADMIN")

                .requestMatchers(HttpMethod.POST, "/api/v1/reembolsos/**").hasAnyRole("ESTUDIANTE", "ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/v1/reembolsos/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/v1/reembolsos/**").hasRole("ADMIN")

                .requestMatchers("/api/v1/notificaciones/**").authenticated()
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt ->
                jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())
            ));
        return http.build();
    }

    @Bean
    Converter<Jwt, ? extends AbstractAuthenticationToken> jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(this::realmRoles);
        return converter;
    }

    private Collection<GrantedAuthority> realmRoles(Jwt jwt) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess == null) {
            return authorities;
        }
        Object rolesObject = realmAccess.get("roles");
        if (rolesObject instanceof Collection<?> roles) {
            roles.stream()
                .map(String::valueOf)
                .map(role -> "ROLE_" + role)
                .map(SimpleGrantedAuthority::new)
                .forEach(authorities::add);
        }
        return authorities;
    }
}
