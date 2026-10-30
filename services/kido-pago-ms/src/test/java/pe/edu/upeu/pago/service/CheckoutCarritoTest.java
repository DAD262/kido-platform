package pe.edu.upeu.pago.service;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.util.ReflectionTestUtils;
import pe.edu.upeu.pago.client.*;
import pe.edu.upeu.pago.dto.*;
import pe.edu.upeu.pago.entity.*;
import pe.edu.upeu.pago.event.PagoAprobadoEvento;
import pe.edu.upeu.pago.messaging.PagoEventosProducer;
import pe.edu.upeu.pago.repository.*;
import java.math.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

class CheckoutCarritoTest {
 CompraCarritoRepository compras;OrdenCompraRepository ordenes;MovimientoSaldoRepository movimientos;MercadoPagoClient mp;PagoEventosProducer eventos;
 CarritoService carrito;CheckoutService disponibilidad;ComercioService comercio;ConciliacionPagoService conciliacion;
 PagoCarritoValidador validador;PagoService pagos;CheckoutCarritoService service;CompraCarrito grupo;List<OrdenCompra> lineas;boolean persistido;
 @BeforeEach void setup(){
  compras=mock(CompraCarritoRepository.class);ordenes=mock(OrdenCompraRepository.class);movimientos=mock(MovimientoSaldoRepository.class);mp=mock(MercadoPagoClient.class);eventos=mock(PagoEventosProducer.class);carrito=mock(CarritoService.class);disponibilidad=mock(CheckoutService.class);comercio=mock(ComercioService.class);conciliacion=mock(ConciliacionPagoService.class);
  validador=new PagoCarritoValidador(compras,ordenes,mp);
  @SuppressWarnings("unchecked") ObjectProvider<CheckoutCarritoService> provider=mock(ObjectProvider.class);
  pagos=new PagoService(ordenes,movimientos,mock(CursoClient.class),eventos,mp,mock(CertificadoPagoService.class),mock(LiquidacionService.class),conciliacion,provider,validador);
  ReflectionTestUtils.setField(pagos,"comision",new BigDecimal("0.20"));ReflectionTestUtils.setField(pagos,"diasRetencion",7L);
  service=new CheckoutCarritoService(carrito,compras,ordenes,pagos,mp,validador,disponibilidad,comercio,conciliacion);when(provider.getObject()).thenReturn(service);
  grupo=new CompraCarrito();grupo.setId(1L);grupo.setEstudianteId(7L);grupo.setAmbiente(AmbientePago.PRUEBAS);grupo.setReceptorMp("123");grupo.setTotal(new BigDecimal("159.80"));grupo.setEstado("PAGO_INICIADO");grupo.setMercadoPagoPreferenceId("pref");
  lineas=new ArrayList<>(List.of(linea(101L,12L,"89.90"),linea(102L,15L,"69.90")));persistido=true;
  when(compras.bloquear(1L)).thenAnswer(a->Optional.of(grupo));when(compras.findById(1L)).thenAnswer(a->Optional.of(grupo));when(compras.saveAndFlush(any())).thenAnswer(a->{grupo=a.getArgument(0);grupo.setId(1L);persistido=true;return grupo;});
  when(compras.findByEstudianteIdAndHuellaOrderByIdDesc(eq(7L),anyString())).thenAnswer(a->persistido?List.of(grupo):List.of());
  when(ordenes.findByCompraCarritoIdOrderByIdAsc(1L)).thenAnswer(a->List.copyOf(lineas));when(ordenes.bloquear(anyLong())).thenAnswer(a->lineas.stream().filter(o->o.getId().equals(a.getArgument(0))).findFirst());
  AtomicLong ids=new AtomicLong(100);when(ordenes.save(any())).thenAnswer(a->{OrdenCompra o=a.getArgument(0);if(o.getId()==null){o.setId(ids.incrementAndGet());lineas.add(o);}return o;});
  doCallRealMethod().when(mp).validarPago(any(),anyString(),any());when(mp.obtenerPago("555")).thenAnswer(a->pago("159.80","PEN","123",false,BigDecimal.ZERO));
 }
 OrdenCompra linea(Long id,Long curso,String precio){var o=new OrdenCompra();o.setId(id);o.setCompraCarritoId(1L);o.setCursoId(curso);o.setCursoTitulo("Curso "+curso);o.setEstudianteId(7L);o.setDocenteId(curso);o.setAmbiente(AmbientePago.PRUEBAS);o.setReceptorMp("123");o.setMontoTotal(new BigDecimal(precio));o.setMontoDocente(o.getMontoTotal().multiply(new BigDecimal("0.80")));o.setComisionKido(o.getMontoTotal().subtract(o.getMontoDocente()));o.setEstado(OrdenCompra.EstadoOrden.PAGO_INICIADO);return o;}
 MercadoPagoClient.PagoExterno pago(String monto,String moneda,String receptor,boolean live,BigDecimal reembolso){return new MercadoPagoClient.PagoExterno("555","approved","CARRITO-1",monto,moneda,new BigDecimal("6.00"),new BigDecimal("153.80"),"released",null,reembolso,receptor,live,"visa","accredited","2026-10-29T16:00:00-05:00");}
 @Test void unaOperacionDePruebaActivaDosOrdenesSinSaldoRealYRepetidaNoDuplicaEventos(){
  service.notificar(pago("159.80","PEN","123",false,BigDecimal.ZERO));service.notificar(pago("159.80","PEN","123",false,BigDecimal.ZERO));
  assertEquals("APROBADA",grupo.getEstado());assertTrue(lineas.stream().allMatch(o->o.getEstado()==OrdenCompra.EstadoOrden.APROBADA&&"555".equals(o.getMercadoPagoPaymentId())));
  var captura=ArgumentCaptor.forClass(PagoAprobadoEvento.class);verify(eventos,times(2)).publicarTrasCommit(captura.capture());assertEquals(Set.of(101L,102L),new HashSet<>(captura.getAllValues().stream().map(PagoAprobadoEvento::getOrdenId).toList()));assertTrue(captura.getAllValues().stream().allMatch(e->"PRUEBAS".equals(e.getAmbiente())&&e.getCompraCarritoId()==1L&&"555".equals(e.getOperacionPago())));verifyNoInteractions(movimientos);
 }
 @Test void webhookAgrupadoUsaLaMismaConfirmacionIdempotente(){pagos.procesarWebhook("555");pagos.procesarWebhook("555");verify(eventos,times(2)).publicarTrasCommit(any());verifyNoInteractions(movimientos);}
 @Test void produccionReparteImportesSinDuplicarDinero(){grupo.setAmbiente(AmbientePago.PRODUCCION);lineas.forEach(o->o.setAmbiente(AmbientePago.PRODUCCION));when(mp.obtenerPago("555")).thenReturn(pago("159.80","PEN","123",true,BigDecimal.ZERO));service.confirmar(1L,"555");var captura=ArgumentCaptor.forClass(MovimientoSaldo.class);verify(movimientos,times(2)).save(captura.capture());assertEquals(0,new BigDecimal("127.84").compareTo(captura.getAllValues().stream().map(MovimientoSaldo::getMonto).reduce(BigDecimal.ZERO,BigDecimal::add)));assertEquals(0,new BigDecimal("6.00").compareTo(lineas.stream().map(OrdenCompra::getCostoProcesamiento).reduce(BigDecimal.ZERO,BigDecimal::add)));assertEquals(0,new BigDecimal("153.80").compareTo(lineas.stream().map(OrdenCompra::getNetoRecibido).reduce(BigDecimal.ZERO,BigDecimal::add)));}
 @Test void importeOMonedaIncorrectosNoAprueban(){for(var p:List.of(pago("89.90","PEN","123",false,BigDecimal.ZERO),pago("159.80","USD","123",false,BigDecimal.ZERO))){when(mp.obtenerPago("555")).thenReturn(p);assertThrows(IllegalArgumentException.class,()->service.confirmar(1L,"555"));}verifyNoInteractions(eventos,movimientos);assertEquals("PAGO_INICIADO",grupo.getEstado());}
 @Test void receptorIncorrectoNoAprueba(){when(mp.obtenerPago("555")).thenReturn(pago("159.80","PEN","999",false,BigDecimal.ZERO));assertThrows(IllegalArgumentException.class,()->service.confirmar(1L,"555"));verifyNoInteractions(eventos,movimientos);}
 @Test void referenciaDeOrdenIndividualNoApruebaCarrito(){when(mp.obtenerPago("555")).thenReturn(new MercadoPagoClient.PagoExterno("555","approved","101","159.80","PEN",null,null,null,null,BigDecimal.ZERO,"123",false));assertThrows(IllegalArgumentException.class,()->service.confirmar(1L,"555"));verifyNoInteractions(eventos,movimientos);}
 @Test void devolucionDePruebaRevocaAccesosSinProducirDineroReal(){service.confirmar(1L,"555");var p=new MercadoPagoClient.PagoExterno("555","refunded","CARRITO-1","159.80","PEN",null,null,null,null,new BigDecimal("159.80"),"123",false);service.notificar(p);assertEquals("REEMBOLSADA",service.obtener(1L).estado());assertTrue(lineas.stream().allMatch(OrdenCompra::isRequiereRevocacion));verifyNoInteractions(movimientos);}
 @Test void ambienteDesconocidoNoGeneraSaldo(){grupo.setAmbiente(AmbientePago.DESCONOCIDO);assertThrows(IllegalArgumentException.class,()->service.confirmar(1L,"555"));verifyNoInteractions(eventos,movimientos);}
 @Test void produccionRechazaPagoConAmbienteDePruebas(){grupo.setAmbiente(AmbientePago.PRODUCCION);lineas.forEach(o->o.setAmbiente(AmbientePago.PRODUCCION));assertThrows(IllegalArgumentException.class,()->service.confirmar(1L,"555"));verifyNoInteractions(eventos,movimientos);}
 @Test void precioDeLineaAlteradoInvalidaTodoElPago(){lineas.getFirst().setMontoTotal(new BigDecimal("1"));assertThrows(IllegalStateException.class,()->service.confirmar(1L,"555"));verifyNoInteractions(eventos,movimientos);}
 @Test void pagoConDevolucionNoSeAprueba(){when(mp.obtenerPago("555")).thenReturn(pago("159.80","PEN","123",false,BigDecimal.ONE));assertThrows(IllegalStateException.class,()->service.confirmar(1L,"555"));verifyNoInteractions(eventos,movimientos);}
 @Test void compraParalelaYaAcreditadaBloqueaDuplicarElAcceso(){when(ordenes.existsByEstudianteIdAndCursoIdAndEstado(7L,12L,OrdenCompra.EstadoOrden.APROBADA)).thenReturn(true);assertThrows(IllegalStateException.class,()->service.confirmar(1L,"555"));verifyNoInteractions(eventos,movimientos);}
 @Test void comercialIncompletoEnProduccionBloqueaAntesDeCrear(){doThrow(new IllegalStateException("Información comercial incompleta")).when(disponibilidad).exigirActivacion();assertThrows(IllegalStateException.class,()->service.iniciar(7L,"sujeto",new CheckoutCarritoService.Solicitud(List.of(12L,15L),new BigDecimal("159.80"),new AceptacionRequest(true,1L))));verifyNoInteractions(carrito,eventos,movimientos);verify(compras,never()).saveAndFlush(any());verify(mp,never()).crearPreferenciaCarrito(anyList(),anyString());}
 @Test void unaPreferenciaIncluyeAmbosCursosYSeReutilizaEnElReintento(){
  persistido=false;lineas.clear();var datos=List.of(curso(12L,"89.90"),curso(15L,"69.90"));when(carrito.validarTodos(eq(7L),anyList(),any())).thenReturn(datos);when(mp.verificarCuentaConfigurada()).thenReturn(new MercadoPagoClient.CuentaVerificada("123",AmbientePago.PRUEBAS));when(disponibilidad.excepcionLocal()).thenReturn(true);var estado=mock(EstadoComercial.class);when(estado.lista()).thenReturn(false);when(comercio.obtener(true)).thenReturn(estado);when(mp.crearPreferenciaCarrito(anyList(),eq("CARRITO-1"))).thenReturn(new PreferenciaResponse("pref","https://www.mercadopago.com.pe/checkout/","https://www.mercadopago.com.pe/checkout/","https://www.mercadopago.com.pe/checkout/",false));
  var req=new CheckoutCarritoService.Solicitud(List.of(12L,15L),new BigDecimal("159.80"),new AceptacionRequest(true,1L));var una=service.iniciar(7L,"sujeto",req);var dos=service.iniciar(7L,"sujeto",req);assertEquals(una.id(),dos.id());assertEquals(2,lineas.size());assertEquals(new BigDecimal("159.80"),una.total());verify(mp,times(1)).crearPreferenciaCarrito(argThat(xs->xs.size()==2&&xs.stream().map(OrdenCompra::getMontoTotal).reduce(BigDecimal.ZERO,BigDecimal::add).compareTo(new BigDecimal("159.80"))==0),eq("CARRITO-1"));verifyNoInteractions(eventos,movimientos);
 }
 CursoCompraDto curso(Long id,String precio){return new CursoCompraDto(id,"Curso "+id,"PAGO",new BigDecimal(precio),id,"PUBLICADO",List.of(id),100,0,true,BigDecimal.ZERO);}
}
