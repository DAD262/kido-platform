package pe.edu.upeu.pago.service;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import pe.edu.upeu.pago.entity.*;
import pe.edu.upeu.pago.repository.*;
import pe.edu.upeu.pago.client.MercadoPagoClient;
import java.util.Optional;
import java.math.BigDecimal;

@ExtendWith(MockitoExtension.class)
class ConstanciaCompraServiceTest {
 @Mock OrdenCompraRepository ordenes;@Mock ConstanciaCompraRepository constancias;@Mock ConfiguracionComercialRepository comercio;@Mock MercadoPagoClient mp;@Mock AccesoCuentaPago acceso;@InjectMocks ConstanciaCompraService service;
 OrdenCompra o;
 @BeforeEach void setup(){o=new OrdenCompra();o.setId(29L);o.setEstudianteId(1001L);o.setCursoTitulo("Python");o.setDocenteId(1L);o.setEstado(OrdenCompra.EstadoOrden.APROBADA);o.setAmbiente(AmbientePago.PRUEBAS);o.setReceptorMp("123");o.setMercadoPagoPaymentId("456");o.setMontoTotal(new BigDecimal("50.00"));when(ordenes.findById(29L)).thenReturn(Optional.of(o));}
 @Test void propietarioSeCompruebaAntesDeDevolverSnapshot(){doThrow(new AccessDeniedException("ajeno")).when(acceso).verificar(1001L);assertThrows(AccessDeniedException.class,()->service.obtener(29L));verifyNoInteractions(constancias,mp);}
 @Test void pendienteNoTieneConstancia(){when(ordenes.bloquear(29L)).thenReturn(Optional.of(o));o.setEstado(OrdenCompra.EstadoOrden.PAGO_INICIADO);assertThrows(IllegalStateException.class,()->service.obtener(29L));verifyNoInteractions(constancias,mp);}
 @Test void snapshotSeReutilizaSinCambiarDatos(){when(ordenes.bloquear(29L)).thenReturn(Optional.of(o));var c=new ConstanciaCompra();c.setOrdenId(29L);c.setAlumnoNombre("Nombre al emitir");when(constancias.findById("456")).thenReturn(Optional.of(c));assertSame(c,service.obtener(29L));verifyNoInteractions(mp);verify(constancias,never()).save(any());}
 @Test void emiteConDatosRealesDelPerfilYProveedor(){when(ordenes.bloquear(29L)).thenReturn(Optional.of(o));when(mp.obtenerPago("456")).thenReturn(new MercadoPagoClient.PagoExterno("456","approved","29","50","PEN",null,null,null,null,null,"123",true,"account_money","accredited","2026-10-29T12:00:00-04:00"));when(acceso.datosComprador(1001L)).thenReturn(new AccesoCuentaPago.Persona("Ana Estudiante","ana@example.invalid",""));when(constancias.save(any())).thenAnswer(x->x.getArgument(0));var c=service.obtener(29L);assertEquals("Ana Estudiante",c.getAlumnoNombre());assertEquals("account_money",c.getMedioPago());assertEquals(11,c.getFechaPago().getHour());assertEquals("PRUEBAS",c.getAmbiente());verify(mp).validarPago(any(),eq("123"),eq(AmbientePago.PRUEBAS));}
}
