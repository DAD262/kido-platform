package pe.edu.upeu.pago.service;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.pago.client.MercadoPagoClient;
import pe.edu.upeu.pago.dto.*;
import pe.edu.upeu.pago.entity.*;
import pe.edu.upeu.pago.repository.*;
import java.math.*;
import java.time.*;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Service @RequiredArgsConstructor
public class CheckoutCarritoService {
 private final CarritoService carrito;private final CompraCarritoRepository compras;private final OrdenCompraRepository ordenes;
 private final PagoService pagos;private final MercadoPagoClient mp;private final PagoCarritoValidador validador;
 private final CheckoutService disponibilidad;private final ComercioService comercio;
 private final ConciliacionPagoService conciliacion;
 public record Solicitud(List<Long> cursos,BigDecimal totalAceptado,AceptacionRequest aceptacion) {}
 public record Resultado(Long id,String estado,BigDecimal total,String ambiente,boolean inscripcionesCompletadas,List<OrdenResponse> ordenes,PreferenciaResponse preferencia,int pagosAprobadosDetectados) {}
 @Transactional public Resultado iniciar(Long estudiante,String sujeto,Solicitud req){
  if(req==null)throw new IllegalArgumentException("Falta el carrito");
  disponibilidad.exigirActivacion();
  var cursos=carrito.validarTodos(estudiante,req.cursos(),req.totalAceptado());
  var cuenta=mp.verificarCuentaConfigurada();
  if(disponibilidad.excepcionLocal()&&cuenta.ambiente()!=AmbientePago.PRUEBAS)throw new IllegalStateException("La excepción local requiere una cuenta de prueba verificada");
  String huella=huella(estudiante+"|"+cuenta+"|"+cursos);
  var anterior=compras.findByEstudianteIdAndHuellaOrderByIdDesc(estudiante,huella).stream().filter(c->Set.of("PENDIENTE","PAGO_INICIADO").contains(c.getEstado())).findFirst();
  CompraCarrito c;
  if(anterior.isPresent()) {c=compras.bloquear(anterior.get().getId()).orElseThrow();if(!Set.of("PENDIENTE","PAGO_INICIADO").contains(c.getEstado()))throw new IllegalStateException("La compra cambió; actualiza tu carrito");}
  else {
   c=new CompraCarrito();c.setEstudianteId(estudiante);c.setAmbiente(cuenta.ambiente());c.setReceptorMp(cuenta.receptor());c.setHuella(huella);c.setTotal(cursos.stream().map(CursoCompraDto::precio).reduce(BigDecimal.ZERO,BigDecimal::add).setScale(2,RoundingMode.UNNECESSARY));c.setEstado("PENDIENTE");c.setFechaCreacion(LocalDateTime.now(ZoneId.of("America/Lima")));c=compras.saveAndFlush(c);
   for(var curso:cursos)pagos.crearLineaCarrito(estudiante,curso,cuenta,c.getId());
  }
  if(c.getAmbiente()!=cuenta.ambiente()||!Objects.equals(c.getReceptorMp(),cuenta.receptor()))throw new IllegalStateException("Cuenta receptora incompatible");
  if(disponibilidad.excepcionLocal()&&!comercio.obtener(true).lista())comercio.aceptarPrueba("CARRITO",c.getId(),estudiante,sujeto,req.aceptacion());else comercio.aceptar("CARRITO",c.getId(),estudiante,sujeto,req.aceptacion());
  if(c.getMercadoPagoPreferenceId()==null){var lineas=ordenes.findByCompraCarritoIdOrderByIdAsc(c.getId());var pref=mp.crearPreferenciaCarrito(lineas,"CARRITO-"+c.getId());c.setMercadoPagoPreferenceId(pref.preferenceId());c.setCheckoutUrl(pref.checkoutUrl());c.setRetornoConfigurado(pref.retornoConfigurado());c.setEstado("PAGO_INICIADO");for(var o:lineas){o.setEstado(OrdenCompra.EstadoOrden.PAGO_INICIADO);o.setMercadoPagoPreferenceId(pref.preferenceId());ordenes.save(o);}compras.save(c);}
  return respuesta(c);
 }
 @Transactional(readOnly=true) public Resultado obtener(Long id){return respuesta(compras.findById(id).orElseThrow());}
 public Long propietario(Long id){return compras.findById(id).orElseThrow().getEstudianteId();}
 @Transactional public Resultado confirmar(Long id,String paymentId){
  var c=compras.bloquear(id).orElseThrow();
  if("APROBADA".equals(c.getEstado())){if(!Objects.equals(c.getMercadoPagoPaymentId(),paymentId))throw new IllegalArgumentException("Operación diferente a la compra acreditada");return respuesta(c);}
  if(!"PAGO_INICIADO".equals(c.getEstado()))throw new IllegalStateException("El carrito no admite confirmación");
  var p=mp.obtenerPago(paymentId);if(!Objects.equals(paymentId,p.id())||!"approved".equals(p.status()))throw new IllegalStateException("Mercado Pago no confirma el pago agrupado");
  if(p.reembolsado()!=null&&p.reembolsado().signum()>0)throw new IllegalStateException("El pago presenta una devolución y requiere revisión");
  validador.validar(c,p);
  compras.findByMercadoPagoPaymentId(p.id()).filter(x->!x.getId().equals(c.getId())).ifPresent(x->{throw new IllegalStateException("El pago ya pertenece a otra compra");});
  var lineas=ordenes.findByCompraCarritoIdOrderByIdAsc(id);
  for(var o:lineas)if(ordenes.existsByEstudianteIdAndCursoIdAndEstado(o.getEstudianteId(),o.getCursoId(),OrdenCompra.EstadoOrden.APROBADA))throw new IllegalStateException("Uno de los cursos ya fue adquirido; el pago requiere revisión sin generar saldos");
  c.setMercadoPagoPaymentId(p.id());c.setEstado("APROBADA");compras.saveAndFlush(c);
  for(var o:lineas)pagos.aprobarLineaCarrito(o.getId(),id,p);
  return respuesta(c);
 }
 @Transactional public Resultado verificar(Long id){var c=compras.bloquear(id).orElseThrow();if("APROBADA".equals(c.getEstado()))return respuesta(c);var cuenta=mp.verificarCuentaConfigurada();if(cuenta.ambiente()!=c.getAmbiente()||!Objects.equals(cuenta.receptor(),c.getReceptorMp()))throw new IllegalStateException("Cuenta receptora incompatible");var ids=mp.pagosAprobadosDeOrden("CARRITO-"+id);c.setPagosAprobadosDetectados(ids.size());compras.save(c);if(ids.isEmpty())return respuesta(c);return confirmar(id,ids.getFirst());}
 @Transactional public void notificar(MercadoPagoClient.PagoExterno p){
  Long id=Long.valueOf(p.externalReference().substring(8));
  if("approved".equals(p.status()))confirmar(id,p.id());else {var c=compras.findById(id).orElseThrow();validador.validar(c,p);}
  for(var o:ordenes.findByCompraCarritoIdOrderByIdAsc(id)){
   if(o.getAmbiente()==AmbientePago.PRODUCCION&&o.getMercadoPagoPaymentId()!=null)conciliacion.compra(o.getId());
   else if(o.getAmbiente()==AmbientePago.PRUEBAS&&Objects.equals(o.getMercadoPagoPaymentId(),p.id())&&Set.of("refunded","charged_back").contains(p.status())){o.setEstado("refunded".equals(p.status())?OrdenCompra.EstadoOrden.REEMBOLSADA:OrdenCompra.EstadoOrden.CONTRACARGO);o.setRequiereRevocacion(true);ordenes.save(o);}
  }
 }
 private Resultado respuesta(CompraCarrito c){
  var filas=ordenes.findByCompraCarritoIdOrderByIdAsc(c.getId()).stream().map(pagos::map).toList();
  String estado=c.getEstado();
  if(!filas.isEmpty()&&filas.stream().allMatch(o->"REEMBOLSADA".equals(o.estado())))estado="REEMBOLSADA";
  else if(filas.stream().anyMatch(o->"CONTRACARGO".equals(o.estado())))estado="CONTRACARGO";
  boolean pendiente=Set.of("PENDIENTE","PAGO_INICIADO").contains(estado);
  PreferenciaResponse pref=!pendiente||c.getCheckoutUrl()==null?null:new PreferenciaResponse(c.getMercadoPagoPreferenceId(),c.getCheckoutUrl(),c.getCheckoutUrl(),c.getCheckoutUrl(),c.isRetornoConfigurado());
  return new Resultado(c.getId(),estado,c.getTotal(),c.getAmbiente().name(),!filas.isEmpty()&&filas.stream().allMatch(o->"APROBADA".equals(o.estado())&&o.inscripcionSincronizada()),filas,pref,c.getPagosAprobadosDetectados());
 }
 private String huella(String valor){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(valor.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
}
