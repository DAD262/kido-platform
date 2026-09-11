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
import pe.edu.upeu.pago.repository.*;
import java.math.*;
import java.time.*;
import java.util.*;

@Service @RequiredArgsConstructor
public class PagoService {
 private final OrdenCompraRepository ordenRepo;
 private final MovimientoSaldoRepository movimientoRepo;
 private final CursoClient cursoClient;
 private final InscripcionClient inscripcionClient;
 private final NotificacionClient notificacionClient;
 private final MercadoPagoClient mercadoPagoClient;
 @Value("${kido.pagos.comision:0.10}") private BigDecimal comision;
 @Value("${kido.pagos.dias-retencion:7}") private long diasRetencion;
 @Value("${kido.pagos.permitir-simulacion:false}") private boolean permitirSimulacion;

 @Transactional(readOnly=true) public List<OrdenResponse> listar(){return ordenRepo.findAll().stream().map(this::map).toList();}
 @Transactional(readOnly=true) public OrdenResponse obtener(Long id){return map(buscar(id));}
 @Transactional(readOnly=true) public List<OrdenResponse> porDocente(Long id){return ordenRepo.findByDocenteIdOrderByIdDesc(id).stream().map(this::map).toList();}
 @Transactional(readOnly=true) public List<OrdenResponse> porEstudiante(Long id){return ordenRepo.findByEstudianteIdOrderByIdDesc(id).stream().map(this::map).toList();}

 @Transactional
 public OrdenResponse crear(CrearOrdenRequest req){
   CursoCompraDto c=cursoClient.obtener(req.cursoId());
   if(c==null) throw new ResourceNotFoundException("Curso no encontrado");
   if(!"PAGO".equalsIgnoreCase(c.tipo())) throw new IllegalArgumentException("El curso es gratuito y no requiere orden de pago");
   if(!"PUBLICADO".equalsIgnoreCase(c.estado())) throw new IllegalArgumentException("Solo se puede comprar un curso PUBLICADO");
   if(c.precio()==null || c.precio().compareTo(BigDecimal.ZERO)<=0) throw new IllegalArgumentException("El curso no tiene un precio válido");
   Optional<OrdenCompra> existente=ordenRepo.findFirstByEstudianteIdAndCursoIdAndEstadoInOrderByIdDesc(
       req.estudianteId(), c.id(), List.of(OrdenCompra.EstadoOrden.PENDIENTE, OrdenCompra.EstadoOrden.PAGO_INICIADO, OrdenCompra.EstadoOrden.APROBADA));
   if(existente.isPresent()){
     if(existente.get().getEstado()==OrdenCompra.EstadoOrden.APROBADA) throw new IllegalStateException("El estudiante ya compró este curso");
     return map(existente.get());
   }
   OrdenCompra o=new OrdenCompra(); o.setEstudianteId(req.estudianteId()); o.setCursoId(c.id()); o.setDocenteId(c.docenteId()); o.setCursoTitulo(c.titulo());
   o.setMontoTotal(c.precio().setScale(2,RoundingMode.HALF_UP)); o.setPorcentajeComision(comision.multiply(new BigDecimal("100")).setScale(2,RoundingMode.HALF_UP));
   o.setComisionKido(o.getMontoTotal().multiply(comision).setScale(2,RoundingMode.HALF_UP)); o.setMontoDocente(o.getMontoTotal().subtract(o.getComisionKido()));
   o.setEstado(OrdenCompra.EstadoOrden.PENDIENTE); o.setFechaCreacion(LocalDateTime.now());
   return map(ordenRepo.save(o));
 }

 @Transactional
 public PreferenciaResponse iniciarMercadoPago(Long id){
   OrdenCompra o=buscar(id);
   if(o.getEstado()==OrdenCompra.EstadoOrden.APROBADA) throw new IllegalStateException("La orden ya fue pagada");
   if(o.getEstado()==OrdenCompra.EstadoOrden.REEMBOLSADA || o.getEstado()==OrdenCompra.EstadoOrden.RECHAZADA) throw new IllegalStateException("La orden ya está cerrada y no puede volver a pagarse");
   PreferenciaResponse p=mercadoPagoClient.crearPreferencia(o); o.setMercadoPagoPreferenceId(p.preferenceId()); o.setEstado(OrdenCompra.EstadoOrden.PAGO_INICIADO); ordenRepo.save(o); return p;
 }

 @Transactional
 public OrdenResponse confirmarMercadoPago(Long id,String paymentId){
   OrdenCompra o=buscar(id);
   if(o.getEstado()==OrdenCompra.EstadoOrden.APROBADA) return map(o);
   if(o.getEstado()==OrdenCompra.EstadoOrden.REEMBOLSADA || o.getEstado()==OrdenCompra.EstadoOrden.RECHAZADA) throw new IllegalStateException("La orden está cerrada y no admite confirmación");
   MercadoPagoClient.PagoExterno p=mercadoPagoClient.obtenerPago(paymentId);
   if(!"approved".equalsIgnoreCase(p.status())) throw new IllegalStateException("Mercado Pago aún no reporta el pago como approved: "+p.status());
   if(p.externalReference()!=null && !String.valueOf(id).equals(p.externalReference())) throw new IllegalArgumentException("El pago no corresponde a esta orden");
   if(p.amount()!=null && new BigDecimal(p.amount()).compareTo(o.getMontoTotal())!=0) throw new IllegalArgumentException("El monto aprobado por Mercado Pago no coincide con la orden");
   return aprobar(o,p.id());
 }

 @Transactional
 public OrdenResponse simularAprobacion(Long id){ if(!permitirSimulacion) throw new IllegalStateException("La simulación solo está habilitada en DEV"); return aprobar(buscar(id),"SIM-"+UUID.randomUUID()); }

 @Transactional
 public OrdenResponse procesarWebhook(String paymentId){
   MercadoPagoClient.PagoExterno p=mercadoPagoClient.obtenerPago(paymentId); if(p.externalReference()==null) throw new IllegalArgumentException("Webhook sin external_reference");
   Long id=Long.valueOf(p.externalReference()); if(!"approved".equalsIgnoreCase(p.status())) return obtener(id); return confirmarMercadoPago(id,p.id());
 }

 private OrdenResponse aprobar(OrdenCompra o,String paymentId){
   if(o.getEstado()==OrdenCompra.EstadoOrden.APROBADA) return map(o);
   if(o.getEstado()==OrdenCompra.EstadoOrden.REEMBOLSADA || o.getEstado()==OrdenCompra.EstadoOrden.RECHAZADA) throw new IllegalStateException("La orden está cerrada y no puede aprobarse de nuevo");
   ordenRepo.findByMercadoPagoPaymentId(paymentId).filter(x->!x.getId().equals(o.getId())).ifPresent(x->{throw new IllegalStateException("El paymentId ya fue utilizado");});
   o.setEstado(OrdenCompra.EstadoOrden.APROBADA); o.setMercadoPagoPaymentId(paymentId); o.setFechaAprobacion(LocalDateTime.now()); ordenRepo.save(o);
   if(movimientoRepo.findByOrdenIdAndTipo(o.getId(),MovimientoSaldo.TipoMovimiento.VENTA).isEmpty()){
     MovimientoSaldo m=new MovimientoSaldo(); m.setDocenteId(o.getDocenteId()); m.setOrdenId(o.getId()); m.setTipo(MovimientoSaldo.TipoMovimiento.VENTA); m.setEstado(MovimientoSaldo.EstadoMovimiento.PENDIENTE); m.setMonto(o.getMontoDocente()); m.setFechaDisponible(o.getFechaAprobacion().plusDays(diasRetencion)); m.setFechaCreacion(LocalDateTime.now()); m.setDescripcion("90% de venta del curso "+o.getCursoTitulo()); movimientoRepo.save(m);
   }
   sincronizarInscripcion(o);
   notificacionClient.enviar(o.getEstudianteId(),"PAGO_APROBADO","Pago aprobado","Tu compra de "+o.getCursoTitulo()+" fue aprobada.");
   notificacionClient.enviar(o.getDocenteId(),"NUEVA_VENTA","Nueva venta","Tienes una nueva venta. Tu saldo neto es S/ "+o.getMontoDocente()+" y se libera en "+diasRetencion+" días.");
   return map(o);
 }

 @Transactional
 public OrdenResponse reintentarInscripcion(Long id){OrdenCompra o=buscar(id); if(o.getEstado()!=OrdenCompra.EstadoOrden.APROBADA) throw new IllegalStateException("La orden no está aprobada"); sincronizarInscripcion(o); return map(o);}
 private void sincronizarInscripcion(OrdenCompra o){
   try{CursoCompraDto c=cursoClient.obtener(o.getCursoId()); inscripcionClient.crearCompra(o.getEstudianteId(),o.getCursoId(),c.leccionIds()==null?List.of():c.leccionIds()); o.setInscripcionSincronizada(true); ordenRepo.save(o);}catch(Exception e){o.setInscripcionSincronizada(false); ordenRepo.save(o);}
 }

 @Transactional @Scheduled(fixedDelayString="${kido.pagos.liberacion-ms:60000}")
 public void liberarSaldosVencidos(){movimientoRepo.findByEstadoAndFechaDisponibleLessThanEqual(MovimientoSaldo.EstadoMovimiento.PENDIENTE,LocalDateTime.now()).forEach(m->{m.setEstado(MovimientoSaldo.EstadoMovimiento.DISPONIBLE);movimientoRepo.save(m);});}

 @Transactional(readOnly=true)
 public SaldoResponse saldo(Long docenteId){
   BigDecimal pend=BigDecimal.ZERO,disp=BigDecimal.ZERO,res=BigDecimal.ZERO,ret=BigDecimal.ZERO;
   for(MovimientoSaldo m:movimientoRepo.findByDocenteId(docenteId)){
     switch(m.getEstado()){
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
 public OrdenResponse map(OrdenCompra o){return new OrdenResponse(o.getId(),o.getEstudianteId(),o.getCursoId(),o.getDocenteId(),o.getCursoTitulo(),o.getMontoTotal(),o.getComisionKido(),o.getMontoDocente(),o.getEstado().name(),o.getMercadoPagoPreferenceId(),o.getMercadoPagoPaymentId(),o.isInscripcionSincronizada(),o.getFechaCreacion(),o.getFechaAprobacion());}
}
