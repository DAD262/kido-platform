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

  public record Persona(String nombre,String email,String documento) {}
  public Long estudianteActual() {
    var auth=SecurityContextHolder.getContext().getAuthentication();
    if(!(auth instanceof JwtAuthenticationToken jwt)||!auth.isAuthenticated()||auth.getAuthorities().stream().noneMatch(a->a.getAuthority().equals("ROLE_ESTUDIANTE")))
      throw new AccessDeniedException("El carrito requiere una cuenta de estudiante");
    Map<?,?> perfil=usuarios.get().uri("http://kido-usuario-ms/api/v1/usuarios/me")
      .header("Authorization","Bearer "+jwt.getToken().getTokenValue()).retrieve().body(Map.class);
    if(perfil==null||!(perfil.get("id") instanceof Number id)||id.longValue()<=0)throw new AccessDeniedException("Perfil de estudiante no disponible");
    return ((Number)perfil.get("id")).longValue();
  }
  public Persona datosComprador(Long perfilId) {
    verificar(perfilId);
    var auth=SecurityContextHolder.getContext().getAuthentication();
    if(!(auth instanceof JwtAuthenticationToken jwt))throw new AccessDeniedException("Sesión inválida");
    String bearer="Bearer "+jwt.getToken().getTokenValue();
    Map<?,?> perfil;
    if(auth.getAuthorities().stream().anyMatch(a->a.getAuthority().equals("ROLE_ADMIN"))){
      java.util.List<?> cuentas=usuarios.get().uri("http://kido-usuario-ms/api/v1/usuarios/admin/cuentas").header("Authorization",bearer).retrieve().body(java.util.List.class);
      perfil=cuentas==null?null:cuentas.stream().filter(Map.class::isInstance).map(Map.class::cast)
        .filter(p->p.get("perfilId") instanceof Number n&&n.longValue()==perfilId).findFirst().orElse(null);
    }else perfil=usuarios.get().uri("http://kido-usuario-ms/api/v1/usuarios/me").header("Authorization",bearer).retrieve().body(Map.class);
    if(perfil==null)throw new IllegalStateException("No se encontraron los datos registrados del comprador");
    String nombre=(Objects.toString(perfil.get("nombres"),"")+" "+Objects.toString(perfil.get("apellidos"),"")).strip();
    if(nombre.isBlank())throw new IllegalStateException("Completa los nombres y apellidos del comprador para emitir la constancia");
    return new Persona(nombre,Objects.toString(perfil.get("email"),""),Objects.toString(perfil.get("dni"),""));
  }
}
