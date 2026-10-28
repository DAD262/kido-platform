package pe.edu.upeu.usuario.service;
import org.junit.jupiter.api.Test;import static org.junit.jupiter.api.Assertions.*;import static org.mockito.Mockito.*;import static org.mockito.ArgumentMatchers.*;
import java.util.*;import org.springframework.security.oauth2.jwt.Jwt;import org.springframework.transaction.support.*;
import pe.edu.upeu.usuario.entity.*;import pe.edu.upeu.usuario.repository.*;import pe.edu.upeu.usuario.keycloak.KeycloakAdminClient;
class PerfilServiceSyncTest {
 @Test void sincronizaDesdeIdentidadVigenteSinRestaurarLosDatosDelTokenAntiguo(){
  var perfiles=mock(PerfilRepository.class);var solicitudes=mock(SolicitudDocenteRepository.class);var documentos=mock(DocumentoSolicitudRepository.class);
  var kc=mock(KeycloakAdminClient.class);var tx=mock(TransactionTemplate.class);
  when(tx.execute(any())).thenAnswer(i->((TransactionCallback<?>)i.getArgument(0)).doInTransaction(mock(org.springframework.transaction.TransactionStatus.class)));
  Perfil p=new Perfil("sub","anterior","anterior@example.invalid","Anterior","Rojas",TipoCuenta.ESTUDIANTE);p.setDni("83561924");org.springframework.test.util.ReflectionTestUtils.setField(p,"id",1001L);
  when(perfiles.findByKeycloakId("sub")).thenReturn(Optional.of(p));when(solicitudes.findByPerfilId(1001L)).thenReturn(Optional.empty());
  when(kc.cuenta("sub")).thenReturn(Map.of("username","vigente","email","actual@example.invalid","firstName","Actual","lastName","Rojas"));
  var service=new PerfilService(perfiles,solicitudes,documentos,tx,kc);
  Jwt jwt=Jwt.withTokenValue("prueba").header("alg","RS256").subject("sub").claim("preferred_username","anterior").claim("given_name","Anterior").build();
  var r=service.me(jwt);assertEquals("vigente",r.username());assertEquals("actual@example.invalid",r.email());assertEquals("Actual",r.nombres());assertEquals(1001L,r.id());assertEquals("83561924",r.dni());
 }
}
