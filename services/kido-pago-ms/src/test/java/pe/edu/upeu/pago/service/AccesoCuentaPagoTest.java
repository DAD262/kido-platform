package pe.edu.upeu.pago.service;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import org.junit.jupiter.api.*;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import java.util.List;
class AccesoCuentaPagoTest {
  AccesoCuentaPago acceso; MockRestServiceServer server;
  @BeforeEach void setup(){var builder=RestClient.builder();server=MockRestServiceServer.bindTo(builder).build();acceso=new AccesoCuentaPago(builder);SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(Jwt.withTokenValue("token-prueba").header("alg","RS256").subject("sub-estudiante").build(),List.of(new SimpleGrantedAuthority("ROLE_ESTUDIANTE"))));}
  @AfterEach void clear(){SecurityContextHolder.clearContext();}
  void perfil(){server.expect(requestTo("http://kido-usuario-ms/api/v1/usuarios/me")).andExpect(header("Authorization","Bearer token-prueba")).andRespond(withSuccess("{\"id\":1001}",MediaType.APPLICATION_JSON));}
  @Test void aceptaPerfilPropio(){perfil();acceso.verificar(1001L);server.verify();}
  @Test void rechazaPerfilAjeno(){perfil();assertThrows(AccessDeniedException.class,()->acceso.verificar(2001L));server.verify();}
  @Test void rechazaAnonimo(){SecurityContextHolder.clearContext();assertThrows(AccessDeniedException.class,()->acceso.verificar(1001L));}
  @Test void administradorPuedeGestionarCompras(){var jwt=Jwt.withTokenValue("admin").header("alg","RS256").subject("admin").build();SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt,List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));acceso.verificar(1001L);server.verify();}
}
