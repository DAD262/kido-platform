package pe.edu.upeu.pago.service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.Scheduled;
import pe.edu.upeu.pago.entity.OrdenCompra;
import pe.edu.upeu.pago.repository.OrdenCompraRepository;

/** Reenvía la instantánea de compras ya aprobadas; nunca inicia otro cobro ni fabrica una inscripción. */
@Component @RequiredArgsConstructor @Slf4j
public class RecuperacionAccesosProgramada {
 private final OrdenCompraRepository ordenes;private final PagoService pagos;
 @Scheduled(fixedDelayString="${kido.saga.recuperacion-intervalo-ms:30000}",initialDelayString="30000")
 public void recuperar(){
  for(var orden:ordenes.findTop50ByEstadoAndInscripcionSincronizadaFalseOrderByIdAsc(OrdenCompra.EstadoOrden.APROBADA)){
   if(orden.getEstado()!=OrdenCompra.EstadoOrden.APROBADA||orden.isInscripcionSincronizada())continue;
   try{pagos.reintentarInscripcion(orden.getId());}
   catch(RuntimeException e){log.warn("Acceso de orden {} pendiente de recuperación [{}]",orden.getId(),e.getClass().getSimpleName());}
  }
 }
}
