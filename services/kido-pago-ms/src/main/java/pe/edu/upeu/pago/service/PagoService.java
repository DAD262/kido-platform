package pe.edu.upeu.pago.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.pago.client.*;
import pe.edu.upeu.pago.dto.*;
import pe.edu.upeu.pago.entity.*;
import pe.edu.upeu.pago.exception.ResourceNotFoundException;
import pe.edu.upeu.pago.event.PagoAprobadoEvento;
import pe.edu.upeu.pago.messaging.PagoEventosProducer;
import pe.edu.upeu.pago.repository.*;
import java.math.*;
import java.time.*;
import java.util.*;

@Service @RequiredArgsConstructor
public class PagoService {
 private final OrdenCompraRepository ordenRepo;
 private final MovimientoSaldoRepository movimientoRepo;
 private final CursoClient cursoClient;
 private final PagoEventosProducer pagoEventosProducer;
 private final MercadoPagoClient mercadoPagoClient;
 private final CertificadoPagoService certificadoPagoService;
 private final LiquidacionService liquidaciones;
 private final ConciliacionPagoService conciliacion;
 private final org.springframework.beans.factory.ObjectProvider<CheckoutCarritoService> checkoutCarrito;
 private final PagoCarritoValidador validadorCarrito;
 @Value("${kido.pagos.comision:0.20}") private BigDecimal comision;
 @Value("${kido.pagos.dias-retencion:7}") private long diasRetencion;
 @Value("${kido.pagos.permitir-simulacion:false}") private boolean permitirSimulacion;

 @Transactional(readOnly=true) public List<OrdenResponse> listar(){return ordenRepo.findAll().stream().sorted(Comparator.comparing(OrdenCompra::getId).reversed()).map(this::map).toList();}
 @Transactional(readOnly=true) public OrdenResponse obtener(Long id){return map(buscar(id));}
 @Transactional(readOnly=true) public List<OrdenResponse> porDocente(Long id){return ordenRepo.findByDocenteIdOrderByIdDesc(id).stream().filter(o->o.getAmbiente()==AmbientePago.PRODUCCION).map(this::map).toList();}
 @Transactional(readOnly=true) public List<OrdenResponse> porEstudiante(Long id){return ordenRepo.findByEstudianteIdOrderByIdDesc(id).stream().map(this::map).toList();}

 @Transactional
 public OrdenResponse crear(CrearOrdenRequest req){
   CursoCompraDto c=cursoClient.obtener(req.cursoId());
   if(c==null) throw new ResourceNotFoundException("Curso no encontrado");
   if(!"PAGO".equalsIgnoreCase(c.tipo())) throw new IllegalArgumentException("El curso es gratuito y no requiere orden de pago");
   if(!"PUBLICADO".equalsIgnoreCase(c.estado())) throw new IllegalArgumentException("Solo se puede comprar un curso PUBLICADO");
   if(c.precio()==null || c.precio().compareTo(BigDecimal.ZERO)<=0) throw new IllegalArgumentException("El curso no tiene un precio válido");
   var cuenta=mercadoPagoClient.verificarCuentaConfigurada();
   if(ordenRepo.existsByEstudianteIdAndCursoIdAndEstado(req.estudianteId(),c.id(),OrdenCompra.EstadoOrden.APROBADA))throw new IllegalStateException("El estudiante ya compró este curso");
   Optional<OrdenCompra> existente=ordenRepo.findFirstByEstudianteIdAndCursoIdAndEstadoInOrderByIdDesc(
       req.estudianteId(), c.id(), List.of(OrdenCompra.EstadoOrden.PENDIENTE, OrdenCompra.EstadoOrden.PAGO_INICIADO, OrdenCompra.EstadoOrden.APROBADA));
   existente=existente.filter(o->o.getCompraCarritoId()==null);
   if(existente.isPresent()){
     if(existente.get().getEstado()==OrdenCompra.EstadoOrden.APROBADA) throw new IllegalStateException("El estudiante ya compró este curso");
     var anterior=existente.get();
     if(anterior.getAmbiente()==cuenta.ambiente()&&Objects.equals(anterior.getReceptorMp(),cuenta.receptor()))return map(anterior);
     if(cuenta.ambiente()!=AmbientePago.PRUEBAS||anterior.getEstado()!=OrdenCompra.EstadoOrden.PENDIENTE||anterior.getMercadoPagoPreferenceId()!=null||anterior.getMercadoPagoPaymentId()!=null)
       throw new IllegalStateException("La orden pendiente pertenece a otra cuenta o ambiente y requiere revisión");
   }
   return map(guardarNueva(req,c,cuenta,null));
 }
 private OrdenCompra guardarNueva(CrearOrdenRequest req,CursoCompraDto c,MercadoPagoClient.CuentaVerificada cuenta,Long carrito){
   OrdenCompra o=new OrdenCompra();o.setCompraCarritoId(carrito); o.setAmbiente(cuenta.ambiente()); o.setReceptorMp(cuenta.receptor()); o.setEstudianteId(req.estudianteId()); o.setCursoId(c.id()); o.setDocenteId(c.docenteId()); o.setCursoTitulo(c.titulo());
   o.setMontoTotal(c.precio().setScale(2,RoundingMode.HALF_UP)); o.setPorcentajeComision(comision.multiply(new BigDecimal("100")).setScale(2,RoundingMode.HALF_UP));
   o.setComisionKido(o.getMontoTotal().multiply(comision).setScale(2,RoundingMode.HALF_UP)); o.setMontoDocente(o.getMontoTotal().subtract(o.getComisionKido()));
   o.setLeccionesSnapshot(c.leccionIds()==null ? "" : c.leccionIds().stream()
       .filter(Objects::nonNull).distinct().map(String::valueOf).collect(java.util.stream.Collectors.joining(",")));
   o.setProgresoMinimoSnapshot(c.progresoMinimo()==null ? 100 : c.progresoMinimo());
   o.setAsistenciaMinimaSnapshot(c.asistenciaMinima()==null ? 0 : c.asistenciaMinima());
   o.setCertificadoHabilitadoSnapshot(c.certificadoHabilitado());
   o.setCertificadoCostoSnapshot(c.certificadoCosto()==null ? BigDecimal.ZERO : c.certificadoCosto());
   o.setEstado(OrdenCompra.EstadoOrden.PENDIENTE); o.setFechaCreacion(LocalDateTime.now(ZoneId.of("America/Lima")));
   return ordenRepo.save(o);
 }
 @Transactional public OrdenCompra crearLineaCarrito(Long estudiante,CursoCompraDto c,MercadoPagoClient.CuentaVerificada cuenta,Long carrito){
   if(!"PAGO".equals(c.tipo())||!"PUBLICADO".equals(c.estado())||c.precio()==null||c.precio().signum()<=0||ordenRepo.existsByEstudianteIdAndCursoIdAndEstado(estudiante,c.id(),OrdenCompra.EstadoOrden.APROBADA))throw new IllegalStateException("El curso ya fue adquirido o no está disponible");
   return guardarNueva(new CrearOrdenRequest(estudiante,c.id()),c,cuenta,carrito);
 }
 @Transactional public OrdenResponse aprobarLineaCarrito(Long id,Long grupo,MercadoPagoClient.PagoExterno p){
   var o=ordenRepo.bloquear(id).orElseThrow();if(!Objects.equals(o.getCompraCarritoId(),grupo))throw new IllegalArgumentException("Orden ajena al carrito");
   var parte=validadorCarrito.paraLinea(o,p);ConciliacionPagoService.copiar(o,parte);return aprobar(o,p.id());
 }

 @Transactional
 public PreferenciaResponse iniciarMercadoPago(Long id){
   OrdenCompra o=buscar(id);
   if(o.getCompraCarritoId()!=null)throw new IllegalStateException("Esta compra se paga desde el checkout único del carrito");
   if(o.getEstado()==OrdenCompra.EstadoOrden.APROBADA) throw new IllegalStateException("La orden ya fue pagada");
   if(o.getEstado()==OrdenCompra.EstadoOrden.REEMBOLSADA || o.getEstado()==OrdenCompra.EstadoOrden.RECHAZADA) throw new IllegalStateException("La orden ya está cerrada y no puede volver a pagarse");
   var cuenta=mercadoPagoClient.verificarCuentaConfigurada();
   if(cuenta.ambiente()!=o.getAmbiente()||!Objects.equals(cuenta.receptor(),o.getReceptorMp()))throw new IllegalStateException("La cuenta de Mercado Pago no corresponde a la orden");
   PreferenciaResponse p=mercadoPagoClient.crearPreferencia(o); o.setMercadoPagoPreferenceId(p.preferenceId()); o.setEstado(OrdenCompra.EstadoOrden.PAGO_INICIADO); ordenRepo.save(o); return p;
 }

 @Transactional
 public OrdenResponse verificarProveedor(Long id){
   OrdenCompra o=ordenRepo.bloquear(id).orElseThrow(()->new ResourceNotFoundException("Orden no encontrada"));
   if(o.getCompraCarritoId()!=null){checkoutCarrito.getObject().verificar(o.getCompraCarritoId());return map(o);}
   if(o.getAmbiente()==AmbientePago.DESCONOCIDO||o.getReceptorMp()==null)throw new IllegalStateException("La orden histórica requiere revisión; su ambiente no está verificado");
   if(o.getEstado()!=OrdenCompra.EstadoOrden.PAGO_INICIADO&&o.getEstado()!=OrdenCompra.EstadoOrden.APROBADA)return map(o);
   var cuenta=mercadoPagoClient.verificarCuentaConfigurada();
   if(cuenta.ambiente()!=o.getAmbiente()||!Objects.equals(cuenta.receptor(),o.getReceptorMp()))throw new IllegalStateException("La cuenta no corresponde a la orden");
   var pagos=mercadoPagoClient.pagosAprobadosDeOrden(String.valueOf(id));
   o.setPagosAprobadosDetectados(pagos.size());
   if(pagos.isEmpty())return map(ordenRepo.save(o));
   // Una orden otorga un acceso. Otros cobros se conservan para revisión, nunca se suman.
   String operacion=o.getMercadoPagoPaymentId()!=null?o.getMercadoPagoPaymentId():pagos.getFirst();
   return confirmarMercadoPago(id,operacion);
 }

 @Transactional
 public OrdenResponse confirmarMercadoPago(Long id,String paymentId){
   OrdenCompra o=ordenRepo.bloquear(id).orElseThrow(()->new ResourceNotFoundException("Orden no encontrada: "+id));
   if(o.getCompraCarritoId()!=null){checkoutCarrito.getObject().confirmar(o.getCompraCarritoId(),paymentId);return map(o);}
   if(o.getEstado()==OrdenCompra.EstadoOrden.APROBADA){if(!Objects.equals(o.getMercadoPagoPaymentId(),paymentId))throw new IllegalArgumentException("El pago no corresponde a la orden aprobada");return map(o);}
   if(o.getEstado()==OrdenCompra.EstadoOrden.REEMBOLSADA || o.getEstado()==OrdenCompra.EstadoOrden.RECHAZADA) throw new IllegalStateException("La orden está cerrada y no admite confirmación");
   MercadoPagoClient.PagoExterno p=mercadoPagoClient.obtenerPago(paymentId);
   if(!Objects.equals(paymentId,p.id()))throw new IllegalArgumentException("El identificador del pago no coincide");
   if(!"approved".equalsIgnoreCase(p.status())) throw new IllegalStateException("Mercado Pago aún no reporta el pago como approved: "+p.status());
   if(!String.valueOf(id).equals(p.externalReference()))
     throw new IllegalArgumentException("El pago debe tener external_reference igual a la orden");
   p.validarImporte(o.getMontoTotal());
   mercadoPagoClient.validarPago(p,o.getReceptorMp(),o.getAmbiente());
   ConciliacionPagoService.copiar(o,p);
   return aprobar(o,p.id());
 }

 @Transactional
 public OrdenResponse simularAprobacion(Long id){ if(!permitirSimulacion) throw new IllegalStateException("La simulación solo está habilitada en DEV"); var o=buscar(id);if(o.getAmbiente()==AmbientePago.PRODUCCION)throw new IllegalStateException("No se puede simular una orden de producción");o.setAmbiente(AmbientePago.PRUEBAS);return aprobar(o,"SIM-"+UUID.randomUUID()); }

 @Transactional
 public OrdenResponse procesarWebhook(String paymentId){
   MercadoPagoClient.PagoExterno p=mercadoPagoClient.obtenerPago(paymentId); if(p.externalReference()==null) throw new IllegalArgumentException("Webhook sin external_reference");
   if(p.externalReference().startsWith("CARRITO-")){checkoutCarrito.getObject().notificar(p);return null;}
   if(p.externalReference().startsWith("CERT-")){Long certId=Long.valueOf(p.externalReference().substring(5));if("approved".equalsIgnoreCase(p.status()))certificadoPagoService.procesarWebhook(paymentId,p.externalReference());if("APROBADA".equals(certificadoPagoService.obtener(certId).estado()))conciliacion.certificado(certId);return null;}
   Long id=Long.valueOf(p.externalReference());if("approved".equalsIgnoreCase(p.status()))confirmarMercadoPago(id,p.id());if("APROBADA".equals(obtener(id).estado()))conciliacion.compra(id);return obtener(id);
 }

 private OrdenResponse aprobar(OrdenCompra o,String paymentId){
   if(o.getEstado()==OrdenCompra.EstadoOrden.APROBADA) return map(o);
   if(o.getEstado()==OrdenCompra.EstadoOrden.REEMBOLSADA || o.getEstado()==OrdenCompra.EstadoOrden.RECHAZADA) throw new IllegalStateException("La orden está cerrada y no puede aprobarse de nuevo");
   ordenRepo.findByMercadoPagoPaymentId(paymentId).filter(x->!x.getId().equals(o.getId())).ifPresent(x->{throw new IllegalStateException("El paymentId ya fue utilizado");});
   o.setEstado(OrdenCompra.EstadoOrden.APROBADA); o.setMercadoPagoPaymentId(paymentId); o.setFechaAprobacion(LocalDateTime.now(ZoneId.of("America/Lima"))); ordenRepo.save(o);
   if(o.getAmbiente()==AmbientePago.PRODUCCION && !paymentId.startsWith("SIM-") && movimientoRepo.findByOrdenIdAndTipo(o.getId(),MovimientoSaldo.TipoMovimiento.VENTA).isEmpty()){
     MovimientoSaldo m=new MovimientoSaldo(); m.setAmbiente(AmbientePago.PRODUCCION); m.setDocenteId(o.getDocenteId()); m.setOrdenId(o.getId()); m.setTipo(MovimientoSaldo.TipoMovimiento.VENTA);m.setConciliado(!paymentId.startsWith("SIM-")&&o.getCostoProcesamiento()!=null&&o.getNetoRecibido()!=null); m.setEstado(MovimientoSaldo.EstadoMovimiento.PENDIENTE); m.setMonto(o.getMontoDocente()); m.setFechaDisponible(o.getFechaAprobacion().plusDays(diasRetencion)); m.setFechaCreacion(LocalDateTime.now(ZoneId.of("America/Lima"))); m.setDescripcion("Venta del curso "+o.getCursoTitulo()+"; neto docente S/ "+o.getMontoDocente()); movimientoRepo.save(m);
   }
   o.setInscripcionSincronizada(false);
   ordenRepo.save(o);
   pagoEventosProducer.publicarTrasCommit(construirEventoPago(o));
   return map(o);
 }

 @Transactional
 public OrdenResponse reintentarInscripcion(Long id){
   OrdenCompra o=buscar(id);
   if(o.getEstado()!=OrdenCompra.EstadoOrden.APROBADA || o.isInscripcionSincronizada())
     throw new IllegalStateException("Solo puede reintentarse una compra aprobada pendiente de inscripción");
   pagoEventosProducer.publicarTrasCommit(construirEventoPago(o));
   return map(o);
 }

 @Transactional
 public boolean marcarInscripcionSincronizada(Long ordenId,Long estudianteId,Long cursoId){
   OrdenCompra o=buscar(ordenId);
   validarResultado(o, estudianteId, cursoId);
   if(o.isInscripcionSincronizada()){return false;}
   if(o.getEstado()!=OrdenCompra.EstadoOrden.APROBADA){return false;}
   o.setInscripcionSincronizada(true);
   ordenRepo.save(o);
   return true;
 }

 @Transactional
 public boolean compensarCompraPorFalloInscripcion(Long ordenId,Long estudianteId,Long cursoId,String motivo){
   OrdenCompra o=buscar(ordenId);
   validarResultado(o, estudianteId, cursoId);
   // A late/duplicated failure must never undo an already synchronized enrollment.
   if(o.isInscripcionSincronizada()) return false;
   if(o.getEstado()==OrdenCompra.EstadoOrden.REEMBOLSADA || o.getEstado()==OrdenCompra.EstadoOrden.RECHAZADA){return false;}
   if(o.getEstado()!=OrdenCompra.EstadoOrden.APROBADA){return false;}

   var movimiento = movimientoRepo.findByOrdenIdAndTipo(o.getId(),MovimientoSaldo.TipoMovimiento.VENTA);
   if(movimiento.isPresent()&&movimiento.get().isConciliado())liquidaciones.bloquearPendientes(o.getDocenteId(),"Compensación por fallo de inscripción");
   if(movimiento.isPresent() && (movimiento.get().getEstado()==MovimientoSaldo.EstadoMovimiento.RESERVADO
       || movimiento.get().getEstado()==MovimientoSaldo.EstadoMovimiento.PAGADO)) {
     throw new IllegalStateException("Venta con saldo ya comprometido: requiere conciliación manual antes del reembolso");
   }
   String paymentId=o.getMercadoPagoPaymentId();
   if(paymentId!=null && !paymentId.startsWith("SIM-")){
     if(o.getCompraCarritoId()!=null)validadorCarrito.paraLinea(o,mercadoPagoClient.obtenerPago(paymentId));
     mercadoPagoClient.reembolsar(paymentId,o.getMontoTotal(),"KIDO-SAGA-"+o.getId());
   }

   movimientoRepo.findByOrdenIdAndTipo(o.getId(),MovimientoSaldo.TipoMovimiento.VENTA).ifPresent(m->{
     if(m.getEstado()!=MovimientoSaldo.EstadoMovimiento.REVERTIDO){
       m.setEstado(MovimientoSaldo.EstadoMovimiento.REVERTIDO);
       m.setDescripcion("Venta revertida por compensación Saga: "+(motivo==null?"fallo de inscripción":motivo));
       movimientoRepo.save(m);
     }
   });

   o.setEstado(OrdenCompra.EstadoOrden.REEMBOLSADA);
   o.setInscripcionSincronizada(false);
   ordenRepo.save(o);
   return true;
 }

 @Transactional
 public OrdenResponse republicarEventoPago(Long id){
   if(!permitirSimulacion) throw new IllegalStateException("Solo disponible en DEMO local");
   OrdenCompra o=buscar(id);
   if(o.getMercadoPagoPaymentId()==null || !o.getMercadoPagoPaymentId().startsWith("SIM-"))
     throw new IllegalStateException("La republicación manual solo funciona con compras simuladas");
   if(o.getEstado()!=OrdenCompra.EstadoOrden.APROBADA && o.getEstado()!=OrdenCompra.EstadoOrden.REEMBOLSADA)
     throw new IllegalStateException("La orden no tiene un evento histórico de pago aprobado");
   pagoEventosProducer.publicarTrasCommit(construirEventoPago(o));
   return map(o);
 }

 private PagoAprobadoEvento construirEventoPago(OrdenCompra o) {
   // Reuse the snapshot persisted with the order, not an HTTP call after an external payment.
   List<Long> lecciones = o.getLeccionesSnapshot()==null || o.getLeccionesSnapshot().isBlank()
       ? List.of() : Arrays.stream(o.getLeccionesSnapshot().split(",")).map(Long::valueOf).toList();
   return PagoAprobadoEvento.builder()
       .tipoEvento("pago.aprobado").schemaVersion(2).ordenId(o.getId()).estudianteId(o.getEstudianteId())
       .docenteId(o.getDocenteId()).cursoId(o.getCursoId()).cursoTitulo(o.getCursoTitulo())
       .montoTotal(o.getMontoTotal()).montoDocente(o.getMontoDocente()).diasRetencion(diasRetencion)
       .compraCarritoId(o.getCompraCarritoId()).operacionPago(o.getMercadoPagoPaymentId()).estado("APROBADA").ambiente(o.getAmbiente().name()).receptorMp(o.getReceptorMp()).origen("kido-pago-ms").timestamp(Instant.now().toEpochMilli())
       .leccionIds(lecciones).progresoMinimo(o.getProgresoMinimoSnapshot())
       .asistenciaMinima(o.getAsistenciaMinimaSnapshot()).certificadoHabilitado(o.isCertificadoHabilitadoSnapshot())
       .certificadoCosto(o.getCertificadoCostoSnapshot()).build();
 }

 private void validarResultado(OrdenCompra o,Long estudianteId,Long cursoId) {
   if(!Objects.equals(o.getEstudianteId(),estudianteId)||!Objects.equals(o.getCursoId(),cursoId))
     throw new IllegalArgumentException("El resultado académico no corresponde a la orden " + o.getId());
 }

 public void liberarSaldosVencidos(){ /* La liberación requiere conciliación de Mercado Pago; no basta el reloj local. */ }

 @Transactional(readOnly=true)
 public SaldoResponse saldo(Long docenteId){
   BigDecimal pend=BigDecimal.ZERO,disp=BigDecimal.ZERO,res=BigDecimal.ZERO,ret=BigDecimal.ZERO;
   for(MovimientoSaldo m:movimientoRepo.findByDocenteId(docenteId)){
     if(!m.isConciliado())continue;
     if(m.getAmbiente()!=AmbientePago.PRODUCCION)continue;
     switch(m.getEstado()){
       case BLOQUEADO -> pend=pend.add(m.getMonto());
       case PENDIENTE -> pend=pend.add(m.getMonto());
       case DISPONIBLE -> disp=disp.add(m.getMonto());
       case RESERVADO -> {disp=disp.add(m.getMonto()); res=res.add(m.getMonto().abs());}
       case PAGADO -> {disp=disp.add(m.getMonto()); ret=ret.add(m.getMonto().abs());}
       case REVERTIDO -> {}
     }
   }
   return new SaldoResponse(docenteId,pend.setScale(2),disp.setScale(2),res.setScale(2),ret.setScale(2));
 }

 private OrdenCompra buscar(Long id){return ordenRepo.findById(id).orElseThrow(()->new ResourceNotFoundException("Orden no encontrada: "+id));}
 public OrdenResponse map(OrdenCompra o){return new OrdenResponse(o.getId(),o.getEstudianteId(),o.getCursoId(),o.getDocenteId(),o.getCursoTitulo(),o.getMontoTotal(),o.getComisionKido(),o.getMontoDocente(),o.getEstado().name(),o.getMercadoPagoPreferenceId(),o.getMercadoPagoPaymentId(),o.isInscripcionSincronizada(),o.getFechaCreacion(),o.getFechaAprobacion(),o.getPorcentajeComision(),o.getCostoProcesamiento(),o.getNetoRecibido(),o.getEstado()!=OrdenCompra.EstadoOrden.APROBADA||o.getNetoRecibido()==null?null:o.getNetoRecibido().subtract(o.getMontoDocente()),o.getFondosEstado(),o.getFechaConciliacion(),o.getMercadoPagoPaymentId()!=null&&o.getMercadoPagoPaymentId().startsWith("SIM-"),o.getAmbiente().name(),o.getPagosAprobadosDetectados(),o.getCompraCarritoId());}
}
