package pe.edu.upeu.pago.service;
import static org.junit.jupiter.api.Assertions.*;import static org.mockito.Mockito.*;
import java.util.Optional;import org.junit.jupiter.api.*;import org.junit.jupiter.api.extension.ExtendWith;import org.mockito.*;import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.upeu.pago.entity.*;import pe.edu.upeu.pago.dto.*;import pe.edu.upeu.pago.repository.*;
@ExtendWith(MockitoExtension.class) class ComercioServiceTest {
 @Mock ConfiguracionComercialRepository repo;@Mock AceptacionCompraRepository aceptaciones;@InjectMocks ComercioService service;ConfiguracionComercial c;
 @BeforeEach void setup(){c=new ConfiguracionComercial();c.setId(1L);c.setVersion(3L);lenient().when(repo.findById(1L)).thenReturn(Optional.of(c));}
 void completa(){c.setRazonSocial("Vendedor para pruebas automatizadas");c.setRuc("20123456789");c.setDireccion("Domicilio para pruebas");c.setEmailSoporte("atencion@example.invalid");c.setTerminos("Condiciones para pruebas. ".repeat(6));c.setPrivacidad("Privacidad para pruebas. ".repeat(6));c.setReembolsos("Reembolsos para pruebas. ".repeat(6));c.setReclamacionesUrl("https://example.invalid/libro");c.setPublicada(true);}
 @Test void borradorNoExponeTextosAlPublico(){c.setTerminos("Borrador privado");var estado=service.obtener(false);assertFalse(estado.lista());assertEquals("",estado.datos().terminos());assertFalse(estado.pendientes().isEmpty());}
 @Test void impidePublicarSinDatos(){var d=new DatosComerciales(3L,"","","","","","","","",true);assertThrows(IllegalArgumentException.class,()->service.guardar(d));verify(repo,never()).saveAndFlush(any());}
 @Test void noAceptaUnaVersionAnterior(){completa();assertThrows(IllegalArgumentException.class,()->service.aceptar("CURSO",42L,1001L,"sub",new AceptacionRequest(true,2L)));verifyNoInteractions(aceptaciones);}
 @Test void exigeAceptacionExplicita(){completa();assertThrows(IllegalArgumentException.class,()->service.aceptar("CURSO",42L,1001L,"sub",new AceptacionRequest(false,3L)));verifyNoInteractions(aceptaciones);}
 @Test void registraTextosVersionYTitular(){completa();service.aceptar("CURSO",42L,1001L,"sub",new AceptacionRequest(true,3L));var cap=ArgumentCaptor.forClass(AceptacionCompra.class);verify(aceptaciones).save(cap.capture());var a=cap.getValue();assertEquals(3L,a.getVersionComercial());assertEquals(1001L,a.getEstudianteId());assertEquals("sub",a.getSujeto());assertTrue(a.getTextoAceptado().contains(c.getTerminos()));assertNotNull(a.getFecha());}
 @Test void repeticionNoDuplicaAceptacion(){completa();when(aceptaciones.existsByTipoAndOrdenIdAndSujetoAndVersionComercial("CURSO",42L,"sub",3L)).thenReturn(true);service.aceptar("CURSO",42L,1001L,"sub",new AceptacionRequest(true,3L));verify(aceptaciones,never()).save(any());}
 @Test void soloAceptaLibroHttps(){assertFalse(ComercioService.https("javascript:alert(1)"));assertFalse(ComercioService.https("http://example.invalid/libro"));assertFalse(ComercioService.https("https://localhost/libro"));assertTrue(ComercioService.https("https://example.invalid/libro"));}
}
