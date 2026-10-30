package pe.edu.upeu.pago.service;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import pe.edu.upeu.pago.client.MercadoPagoClient;
import pe.edu.upeu.pago.dto.*;
import pe.edu.upeu.pago.entity.*;
import pe.edu.upeu.pago.repository.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CheckoutInternoServiceTest {
 @Mock PagoService pagos;@Mock CertificadoPagoService certificados;@Mock CheckoutCarritoService carritos;@Mock CheckoutService checkout;@Mock ComercioService comercio;@Mock AccesoCuentaPago acceso;@Mock MercadoPagoClient mp;@Mock IntentoCheckoutStore intentos;
 @Mock OrdenCompraRepository ordenes;@Mock OrdenCertificadoRepository ordenesCertificado;@Mock CompraCarritoRepository compras;
 @InjectMocks CheckoutInternoService servicio;
 final String clave="6c104d0c-8dde-4bc2-b595-1aba98456893";
 @BeforeEach void preparar(){
  ReflectionTestUtils.setField(servicio,"publicKey","PUBLIC_KEY_DE_PRUEBA");
  when(carritos.propietario(7L)).thenReturn(3L);
 }
 void compra(){
  var c=mock(CheckoutCarritoService.Resultado.class);when(carritos.obtener(7L)).thenReturn(c);
  when(c.total()).thenReturn(new BigDecimal("149.80"));when(c.estado()).thenReturn("PAGO_INICIADO");when(c.ambiente()).thenReturn("PRUEBAS");
 }
 void proveedor(){
  when(mp.verificarCuentaConfigurada()).thenReturn(new MercadoPagoClient.CuentaVerificada("9",AmbientePago.PRUEBAS));
  var compra=new CompraCarrito();compra.setReceptorMp("9");when(compras.findById(7L)).thenReturn(Optional.of(compra));
 }
 CheckoutInternoService.Solicitud solicitud(String medio){return new CheckoutInternoService.Solicitud(clave,medio,"token_seguro_prueba",1,null,new CheckoutInternoService.Pagador("comprador@example.test",null,null,null),new AceptacionRequest(true,1L));}
 @Test void sinPublicKeyNoCreaCobros(){compra();ReflectionTestUtils.setField(servicio,"publicKey","");assertThrows(IllegalStateException.class,()->servicio.pagar("carrito",7L,"sujeto",solicitud("yape")));verifyNoInteractions(mp,intentos);}
 @Test void noPermiteCompraAjena(){doThrow(new org.springframework.security.access.AccessDeniedException("Ajena")).when(acceso).verificar(3L);assertThrows(org.springframework.security.access.AccessDeniedException.class,()->servicio.pagar("carrito",7L,"sujeto",solicitud("yape")));verifyNoInteractions(mp,intentos);}
 @Test void noCobraAUnReceptorDistinto(){compra();proveedor();when(mp.verificarCuentaConfigurada()).thenReturn(new MercadoPagoClient.CuentaVerificada("10",AmbientePago.PRUEBAS));assertThrows(IllegalStateException.class,()->servicio.pagar("carrito",7L,"sujeto",solicitud("yape")));verifyNoInteractions(intentos);verify(mp,never()).crearPagoInterno(anyMap(),anyString());}
 @Test void rechazaMedioNoHabilitado(){compra();proveedor();when(mp.mediosDisponibles()).thenReturn(List.of());assertThrows(IllegalArgumentException.class,()->servicio.pagar("carrito",7L,"sujeto",solicitud("inventado")));verifyNoInteractions(intentos);}
 @Test void usaTotalDelServidorYNoConfirmaPendientes(){
  compra();
  proveedor();when(mp.mediosDisponibles()).thenReturn(List.of(new MetodoPagoResponse.Medio("yape","Yape","debit_card")));
  when(intentos.iniciar(eq("CARRITO-7"),eq(clave),anyString())).thenReturn(new IntentoCheckoutStore.Intento(clave,"huella","ENVIANDO",null));
  when(mp.crearPagoInterno(anyMap(),eq(clave))).thenReturn(Map.of("id",123,"status","pending","status_detail","pending_review_manual"));
  var r=servicio.pagar("carrito",7L,"sujeto",solicitud("yape"));assertFalse(r.confirmado());
  @SuppressWarnings("unchecked") ArgumentCaptor<Map<String,Object>> body=ArgumentCaptor.forClass(Map.class);verify(mp).crearPagoInterno(body.capture(),eq(clave));assertEquals(new BigDecimal("149.80"),body.getValue().get("transaction_amount"));assertEquals("CARRITO-7",body.getValue().get("external_reference"));verify(carritos,never()).confirmar(anyLong(),anyString());
 }
 @Test void reutilizaOperacionPreviaSinSegundoCobro(){
  compra();
  proveedor();when(mp.mediosDisponibles()).thenReturn(List.of(new MetodoPagoResponse.Medio("yape","Yape","debit_card")));
  when(intentos.iniciar(eq("CARRITO-7"),eq(clave),anyString())).thenReturn(new IntentoCheckoutStore.Intento(clave,"huella","approved","123"));
  when(mp.consultarPagoInterno("123")).thenReturn(Map.of("id",123,"status","approved"));
  assertTrue(servicio.pagar("carrito",7L,"sujeto",solicitud("yape")).confirmado());verify(mp,never()).crearPagoInterno(anyMap(),anyString());verify(carritos).confirmar(7L,"123");
 }
}
