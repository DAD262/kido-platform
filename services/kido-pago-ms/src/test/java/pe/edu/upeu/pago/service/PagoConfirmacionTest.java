package pe.edu.upeu.pago.service;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.upeu.pago.client.*;
import pe.edu.upeu.pago.entity.*;
import pe.edu.upeu.pago.messaging.PagoEventosProducer;
import pe.edu.upeu.pago.repository.*;
@ExtendWith(MockitoExtension.class)
class PagoConfirmacionTest {
  @Mock OrdenCompraRepository ordenRepo;
  @Mock MovimientoSaldoRepository movimientoRepo;
  @Mock CursoClient cursoClient;
  @Mock PagoEventosProducer eventos;
  @Mock MercadoPagoClient mp;
  @Mock CertificadoPagoService certificados;
  @Mock ConciliacionPagoService conciliacion;
  @InjectMocks PagoService servicio;
  OrdenCompra orden;
  @BeforeEach void setup(){orden=new OrdenCompra();orden.setId(42L);orden.setAmbiente(AmbientePago.PRODUCCION);orden.setReceptorMp("1234");orden.setEstudianteId(1001L);orden.setCursoId(2L);orden.setDocenteId(1000L);orden.setCursoTitulo("Spring");orden.setMontoTotal(new BigDecimal("79.90"));orden.setComisionKido(new BigDecimal("7.99"));orden.setMontoDocente(new BigDecimal("71.91"));orden.setEstado(OrdenCompra.EstadoOrden.PAGO_INICIADO);when(ordenRepo.bloquear(42L)).thenReturn(Optional.of(orden));lenient().doAnswer(i->{MercadoPagoClient.PagoExterno p=i.getArgument(0);p.validarIdentidad(i.getArgument(1),i.getArgument(2));return null;}).when(mp).validarPago(any(),any(),any());}
  void pago(String estado,String ref,String monto,String moneda){when(mp.obtenerPago("123")).thenReturn(new MercadoPagoClient.PagoExterno("123",estado,ref,monto,moneda,null,null,null,null,null,"1234",true));}
  void sinAprobacion(){assertEquals(OrdenCompra.EstadoOrden.PAGO_INICIADO,orden.getEstado());verify(ordenRepo,never()).save(any());verifyNoInteractions(eventos,movimientoRepo);}
  @Test void pendienteNoHabilitaCurso(){pago("pending","42","79.90","PEN");assertThrows(IllegalStateException.class,()->servicio.confirmarMercadoPago(42L,"123"));sinAprobacion();}
  @Test void pagoDeOtraOrdenNoHabilitaCurso(){pago("approved","43","79.90","PEN");assertThrows(IllegalArgumentException.class,()->servicio.confirmarMercadoPago(42L,"123"));sinAprobacion();}
  @Test void montoIncorrectoNoHabilitaCurso(){pago("approved","42","0.01","PEN");assertThrows(IllegalArgumentException.class,()->servicio.confirmarMercadoPago(42L,"123"));sinAprobacion();}
  @Test void monedaIncorrectaNoHabilitaCurso(){pago("approved","42","79.90","USD");assertThrows(IllegalArgumentException.class,()->servicio.confirmarMercadoPago(42L,"123"));sinAprobacion();}
  @Test void aprobacionVerificadaEsIdempotente(){pago("approved","42","79.90","PEN");when(ordenRepo.findByMercadoPagoPaymentId("123")).thenReturn(Optional.empty());when(movimientoRepo.findByOrdenIdAndTipo(42L,MovimientoSaldo.TipoMovimiento.VENTA)).thenReturn(Optional.empty());assertEquals("APROBADA",servicio.confirmarMercadoPago(42L,"123").estado());assertEquals("APROBADA",servicio.confirmarMercadoPago(42L,"123").estado());verify(mp,times(1)).obtenerPago("123");verify(movimientoRepo,times(1)).save(any());verify(eventos,times(1)).publicarTrasCommit(any());}
  @Test void pruebaRepetidaNoCreaSaldo(){orden.setAmbiente(AmbientePago.PRUEBAS);when(mp.obtenerPago("123")).thenReturn(new MercadoPagoClient.PagoExterno("123","approved","42","79.90","PEN",null,null,null,null,null,"1234",false));assertEquals("APROBADA",servicio.confirmarMercadoPago(42L,"123").estado());assertEquals("APROBADA",servicio.confirmarMercadoPago(42L,"123").estado());verify(movimientoRepo,never()).save(any());verify(eventos,times(1)).publicarTrasCommit(any());}
  @Test void receptorIncorrectoNoAprueba(){when(mp.obtenerPago("123")).thenReturn(new MercadoPagoClient.PagoExterno("123","approved","42","79.90","PEN",null,null,null,null,null,"9999",true));assertThrows(IllegalArgumentException.class,()->servicio.confirmarMercadoPago(42L,"123"));sinAprobacion();}
  @Test void ordenHistoricaNoApruebaSinIdentidad(){orden.setAmbiente(AmbientePago.DESCONOCIDO);orden.setReceptorMp(null);pago("approved","42","79.90","PEN");assertThrows(IllegalArgumentException.class,()->servicio.confirmarMercadoPago(42L,"123"));sinAprobacion();}
  @Test void webhookPruebaDuplicadoEmiteUnaVezYNoCreaMovimientos(){orden.setAmbiente(AmbientePago.PRUEBAS);when(mp.obtenerPago("123")).thenReturn(new MercadoPagoClient.PagoExterno("123","approved","42","79.90","PEN",null,null,null,null,null,"1234",false));when(ordenRepo.findById(42L)).thenReturn(Optional.of(orden));servicio.procesarWebhook("123");servicio.procesarWebhook("123");verify(eventos,times(1)).publicarTrasCommit(any());verify(movimientoRepo,never()).save(any());verify(conciliacion,times(2)).compra(42L);}
  @Test void webhookRechazaMontoIncorrecto(){pago("approved","42","1.00","PEN");assertThrows(IllegalArgumentException.class,()->servicio.procesarWebhook("123"));sinAprobacion();}
  @Test void webhookRechazaReceptorIncorrecto(){when(mp.obtenerPago("123")).thenReturn(new MercadoPagoClient.PagoExterno("123","approved","42","79.90","PEN",null,null,null,null,null,"9999",true));assertThrows(IllegalArgumentException.class,()->servicio.procesarWebhook("123"));sinAprobacion();}
  @Test void verificarDosPagosDePruebaOtorgaUnSoloAcceso(){orden.setAmbiente(AmbientePago.PRUEBAS);when(mp.verificarCuentaConfigurada()).thenReturn(new MercadoPagoClient.CuentaVerificada("1234",AmbientePago.PRUEBAS));when(mp.pagosAprobadosDeOrden("42")).thenReturn(java.util.List.of("123","124"));when(mp.obtenerPago("123")).thenReturn(new MercadoPagoClient.PagoExterno("123","approved","42","79.90","PEN",null,null,null,null,null,"1234",false));servicio.verificarProveedor(42L);servicio.verificarProveedor(42L);assertEquals(2,orden.getPagosAprobadosDetectados());assertEquals("123",orden.getMercadoPagoPaymentId());verify(eventos,times(1)).publicarTrasCommit(any());verify(movimientoRepo,never()).save(any());}
}
