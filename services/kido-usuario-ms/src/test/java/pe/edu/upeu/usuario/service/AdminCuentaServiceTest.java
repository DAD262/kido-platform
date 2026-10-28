package pe.edu.upeu.usuario.service;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import java.util.*;
import org.springframework.transaction.support.TransactionTemplate;
import pe.edu.upeu.usuario.dto.*;
import pe.edu.upeu.usuario.keycloak.KeycloakAdminClient;
import pe.edu.upeu.usuario.repository.PerfilRepository;
class AdminCuentaServiceTest {
    final KeycloakAdminClient kc=mock(KeycloakAdminClient.class);
    final PerfilRepository perfiles=mock(PerfilRepository.class);
    final TransactionTemplate tx=mock(TransactionTemplate.class);
    final AdminCuentaService service=new AdminCuentaService(kc,perfiles,tx);
    Map<String,Object> cuenta(){return Map.of("id","user","username","persona","email","persona@example.invalid","firstName","Ana","lastName","Rojas","enabled",true,"emailVerified",true);}
    @Test void noPermiteDesactivarse(){
        when(kc.cuenta("user")).thenReturn(cuenta());when(kc.rolesCuenta("user")).thenReturn(List.of("ADMIN"));
        assertThrows(IllegalArgumentException.class,()->service.actualizar("user","user",new ActualizarCuentaRequest("Ana","Rojas","persona@example.invalid",false)));
        verify(kc,never()).actualizarCuenta(anyString(),anyMap());
    }
    @Test void protegeCualquierAdministrador(){
        when(kc.cuenta("user")).thenReturn(cuenta());when(kc.rolesCuenta("user")).thenReturn(List.of("ADMIN"));
        assertThrows(IllegalArgumentException.class,()->service.actualizar("otro","user",new ActualizarCuentaRequest("Ana","Rojas","persona@example.invalid",false)));
        verify(kc,never()).actualizarCuenta(anyString(),anyMap());
    }
    @Test void passwordInvalidaNoLlegaAKeycloak(){
        when(kc.cuenta("user")).thenReturn(cuenta());
        assertThrows(IllegalArgumentException.class,()->service.restablecer("admin","user",new RestablecerPasswordRequest("corta",false)));
        verify(kc,never()).restablecerPassword(anyString(),anyString(),anyBoolean());
    }
    @Test void restableceSoloCuentaSolicitada(){
        when(kc.cuenta("user")).thenReturn(cuenta());
        service.restablecer("admin","user",new RestablecerPasswordRequest("ClavePrueba!927",true));
        verify(kc).restablecerPassword("user","ClavePrueba!927",true);
        verifyNoInteractions(perfiles,tx);
    }
    @Test void excluyeCuentasTecnicas(){
        when(kc.cuenta("service")).thenReturn(Map.of("username","service-account-kido-usuario-admin"));
        assertThrows(IllegalArgumentException.class,()->service.restablecer("admin","service",new RestablecerPasswordRequest("ClavePrueba!927",false)));
        verify(kc,never()).restablecerPassword(anyString(),anyString(),anyBoolean());
    }
    @Test void compensaEdicionSiFallaPerfil(){
        when(kc.cuenta("user")).thenReturn(cuenta());when(kc.rolesCuenta("user")).thenReturn(List.of("ESTUDIANTE"));
        doThrow(new IllegalStateException("Base no disponible")).when(tx).executeWithoutResult(any());
        assertThrows(IllegalStateException.class,()->service.actualizar("admin","user",new ActualizarCuentaRequest("Ana","Perez","nueva@example.invalid",true)));
        verify(kc,times(2)).actualizarCuenta(eq("user"),anyMap());
    }

    @Test void reservaActivaNoSeReutiliza(){
        var historico=new pe.edu.upeu.usuario.entity.Perfil("otra","otra","reservada@example.invalid","Ana","Rojas",pe.edu.upeu.usuario.entity.TipoCuenta.ESTUDIANTE);
        when(kc.cuenta("user")).thenReturn(cuenta());when(kc.rolesCuenta("user")).thenReturn(List.of("ESTUDIANTE"));
        when(perfiles.findByEmailIgnoreCase("reservada@example.invalid")).thenReturn(Optional.of(historico));
        when(kc.cuenta("otra")).thenReturn(Map.of("id","otra"));
        assertThrows(pe.edu.upeu.usuario.exception.ConflictoException.class,()->service.actualizar("admin","user",new ActualizarCuentaRequest("Ana","Rojas","reservada@example.invalid",true)));
        verify(kc,never()).actualizarCuenta(anyString(),anyMap());
    }
    @Test void errorDeIdentidadNoLiberaCorreo(){
        var historico=new pe.edu.upeu.usuario.entity.Perfil("otra","otra","reservada@example.invalid","Ana","Rojas",pe.edu.upeu.usuario.entity.TipoCuenta.ESTUDIANTE);
        when(kc.cuenta("user")).thenReturn(cuenta());when(kc.rolesCuenta("user")).thenReturn(List.of("ESTUDIANTE"));
        when(perfiles.findByEmailIgnoreCase("reservada@example.invalid")).thenReturn(Optional.of(historico));
        when(kc.cuenta("otra")).thenThrow(new pe.edu.upeu.usuario.exception.IdentidadNoDisponibleException("Sin conexion",null));
        assertThrows(pe.edu.upeu.usuario.exception.IdentidadNoDisponibleException.class,()->service.actualizar("admin","user",new ActualizarCuentaRequest("Ana","Rojas","reservada@example.invalid",true)));
        verify(kc,never()).actualizarCuenta(anyString(),anyMap());assertEquals("reservada@example.invalid",historico.getEmail());
    }
    @Test void solo404LiberaCorreoConservandoPerfilHistorico(){
        var historico=new pe.edu.upeu.usuario.entity.Perfil("otra","usuario-antiguo","reservada@example.invalid","Ana","Rojas",pe.edu.upeu.usuario.entity.TipoCuenta.ESTUDIANTE);
        org.springframework.test.util.ReflectionTestUtils.setField(historico,"id",2001L);
        when(kc.cuenta("user")).thenReturn(cuenta());when(kc.rolesCuenta("user")).thenReturn(List.of("ESTUDIANTE"));
        when(perfiles.findByEmailIgnoreCase("reservada@example.invalid")).thenReturn(Optional.of(historico));
        when(perfiles.findById(2001L)).thenReturn(Optional.of(historico));
        when(kc.cuenta("otra")).thenThrow(new pe.edu.upeu.usuario.exception.RecursoNoEncontradoException("No existe"));
        doAnswer(inv->{java.util.function.Consumer<org.springframework.transaction.TransactionStatus> callback=inv.getArgument(0);callback.accept(mock(org.springframework.transaction.TransactionStatus.class));return null;}).when(tx).executeWithoutResult(any());
        service.actualizar("admin","user",new ActualizarCuentaRequest("Ana","Rojas","reservada@example.invalid",true));
        assertNull(historico.getEmail());assertEquals("usuario-antiguo",historico.getUsername());assertEquals(2001L,historico.getId());verify(perfiles,never()).delete(any());
    }

    @Test void permiteActualizarUsuarioYCorreoSinCambiarId(){
        when(kc.cuenta("user")).thenReturn(cuenta());when(kc.rolesCuenta("user")).thenReturn(List.of("ESTUDIANTE"));
        var perfil=new pe.edu.upeu.usuario.entity.Perfil("user","persona","persona@example.invalid","Ana","Rojas",pe.edu.upeu.usuario.entity.TipoCuenta.ESTUDIANTE);
        org.springframework.test.util.ReflectionTestUtils.setField(perfil,"id",1001L);
        when(perfiles.findByKeycloakId("user")).thenReturn(Optional.of(perfil));
        doAnswer(inv->{java.util.function.Consumer<org.springframework.transaction.TransactionStatus> callback=inv.getArgument(0);callback.accept(mock(org.springframework.transaction.TransactionStatus.class));return null;}).when(tx).executeWithoutResult(any());
        service.actualizar("admin","user",new ActualizarCuentaRequest("Ana","Rojas","nueva@example.invalid",true,null,"ana.nueva"));
        assertEquals("ana.nueva",perfil.getUsername());assertEquals("nueva@example.invalid",perfil.getEmail());assertEquals(1001L,perfil.getId());
        verify(kc).actualizarCuenta(eq("user"),argThat(m->"ana.nueva".equals(m.get("username"))&&"nueva@example.invalid".equals(m.get("email"))));
    }
    @Test void rechazaUsuarioDeOtraCuenta(){
        when(kc.cuenta("user")).thenReturn(cuenta());when(kc.rolesCuenta("user")).thenReturn(List.of("ESTUDIANTE"));when(kc.buscarIdPorUsername("ocupado")).thenReturn("otra");
        assertThrows(pe.edu.upeu.usuario.exception.ConflictoException.class,()->service.actualizar("admin","user",new ActualizarCuentaRequest("Ana","Rojas","persona@example.invalid",true,null,"ocupado")));
        verify(kc,never()).actualizarCuenta(anyString(),anyMap());
    }
    @Test void rechazaCorreoQueEsUsuarioDeOtraPersona(){
        when(kc.cuenta("user")).thenReturn(cuenta());when(kc.rolesCuenta("user")).thenReturn(List.of("ESTUDIANTE"));when(kc.buscarIdPorUsername("otra@example.invalid")).thenReturn("otra");
        assertThrows(pe.edu.upeu.usuario.exception.ConflictoException.class,()->service.actualizar("admin","user",new ActualizarCuentaRequest("Ana","Rojas","otra@example.invalid",true,null,"ana")));
        verify(kc,never()).actualizarCuenta(anyString(),anyMap());
    }
    @Test void rechazaUsuarioQueEsCorreoDeOtraPersona(){
        when(kc.cuenta("user")).thenReturn(cuenta());when(kc.rolesCuenta("user")).thenReturn(List.of("ESTUDIANTE"));when(kc.buscarIdPorEmail("otra@example.invalid")).thenReturn("otra");
        assertThrows(pe.edu.upeu.usuario.exception.ConflictoException.class,()->service.actualizar("admin","user",new ActualizarCuentaRequest("Ana","Rojas","persona@example.invalid",true,null,"otra@example.invalid")));
        verify(kc,never()).actualizarCuenta(anyString(),anyMap());
    }
    @Test void impideConvertirPersonaEnCuentaTecnica(){
        when(kc.cuenta("user")).thenReturn(cuenta());when(kc.rolesCuenta("user")).thenReturn(List.of("ESTUDIANTE"));
        assertThrows(IllegalArgumentException.class,()->service.actualizar("admin","user",new ActualizarCuentaRequest("Ana","Rojas","persona@example.invalid",true,null,"service-account-oculta")));
        verify(kc,never()).actualizarCuenta(anyString(),anyMap());
    }
}
