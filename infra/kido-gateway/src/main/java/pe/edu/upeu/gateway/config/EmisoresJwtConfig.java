package pe.edu.upeu.gateway.config;
import java.net.URI;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jwt.*;

/** Firma y tiempos se validan siempre. El emisor del tunel solo se admite hasta su vencimiento. */
@Configuration
public class EmisoresJwtConfig {
 @Bean JwtDecoder jwtDecoder(
  @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri:http://localhost:19090/realms/kido/protocol/openid-connect/certs}") String jwks,
  @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:http://localhost:19090/realms/kido}") String principal,
  @Value("${KIDO_PUBLIC_ORIGIN:}") String publico,@Value("${KIDO_PUBLIC_UNTIL:}") String vence){
  var decoder=NimbusJwtDecoder.withJwkSetUri(jwks).build();
  Instant hasta;try{hasta=Instant.parse(vence);}catch(Exception e){hasta=Instant.EPOCH;}final Instant limite=hasta;
  OAuth2TokenValidator<Jwt> emisor=jwt->permitido(jwt.getClaimAsString("iss"),principal,publico,limite,Instant.now())?OAuth2TokenValidatorResult.success():OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token","Emisor no autorizado o acceso compartido vencido",null));
  decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefault(),emisor));return decoder;
 }
 static boolean permitido(String emisor,String principal,String publico,Instant hasta,Instant ahora){
  if(emisor==null)return false;
  if(new java.util.HashSet<>(java.util.List.of(principal,"http://localhost:4300/auth/realms/kido","http://127.0.0.1:4300/auth/realms/kido","http://[::1]:4300/auth/realms/kido")).contains(emisor))return true;
  if(publico==null||publico.isBlank()||!ahora.isBefore(hasta))return false;
  try{var u=URI.create(publico);if(!"https".equals(u.getScheme())||u.getHost()==null||u.getUserInfo()!=null||u.getQuery()!=null||u.getFragment()!=null||!(u.getPath().isEmpty()||"/".equals(u.getPath())))return false;return emisor.equals(publico.replaceAll("/+$","")+"/auth/realms/kido");}catch(Exception e){return false;}
 }
}
