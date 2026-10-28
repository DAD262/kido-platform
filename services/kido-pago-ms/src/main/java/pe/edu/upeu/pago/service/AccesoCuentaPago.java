package pe.edu.upeu.pago.service;

import java.util.Map;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.client.RestClient;

/** Resuelve el perfil desde el token; nunca confía en el estudianteId enviado por el navegador. */
@Component
public class AccesoCuentaPago {
  private final RestClient usuarios;
  public AccesoCuentaPago(@Qualifier("loadBalancedRestClientBuilder") RestClient.Builder builder) {
    usuarios = builder.clone().build();
  }
  public void verificar(Long perfilId) {
    var auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated()) throw new AccessDeniedException("Debes iniciar sesión");
    if (auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) return;
    if (!(auth instanceof JwtAuthenticationToken jwt)) throw new AccessDeniedException("Sesión inválida");
    Map<?,?> perfil = usuarios.get().uri("http://kido-usuario-ms/api/v1/usuarios/me")
      .header("Authorization", "Bearer " + jwt.getToken().getTokenValue()).retrieve().body(Map.class);
    if (perfil == null || !(perfil.get("id") instanceof Number id) || !Objects.equals(perfilId, id.longValue()))
      throw new AccessDeniedException("No tienes acceso a las compras de esta cuenta");
  }
}
