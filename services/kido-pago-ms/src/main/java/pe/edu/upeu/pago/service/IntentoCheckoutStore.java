package pe.edu.upeu.pago.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.*;

/** Persiste la idempotencia antes de llamar al proveedor; nunca guarda tokens ni datos de tarjeta. */
@Component @RequiredArgsConstructor
public class IntentoCheckoutStore {
 private final JdbcTemplate jdbc;
 public record Intento(String clave,String huella,String estado,String paymentId) {}
 @Transactional(propagation=Propagation.REQUIRES_NEW)
 public Intento iniciar(String referencia,String clave,String huella){
  jdbc.update("INSERT INTO intentos_checkout_interno(referencia,clave,huella,estado) VALUES (?,?,?,'ENVIANDO') ON CONFLICT DO NOTHING",referencia,clave,huella);
  var i=jdbc.queryForObject("SELECT clave,huella,estado,payment_id FROM intentos_checkout_interno WHERE referencia=? FOR UPDATE",(r,n)->new Intento(r.getString(1),r.getString(2),r.getString(3),r.getString(4)),referencia);
  if("rejected".equals(i.estado())||"cancelled".equals(i.estado())){
   if(i.clave().equals(clave))return i;
   jdbc.update("UPDATE intentos_checkout_interno SET clave=?,huella=?,estado='ENVIANDO',payment_id=NULL,actualizado=CURRENT_TIMESTAMP WHERE referencia=?",clave,huella,referencia);
   return new Intento(clave,huella,"ENVIANDO",null);
  }
  if(i.paymentId()!=null)return i;
  if(!i.clave().equals(clave)||!i.huella().equals(huella))throw new IllegalStateException("Tu pago anterior aún se está verificando con Mercado Pago. Revisa Mis compras o vuelve a intentarlo en unos minutos.");
  return i;
 }
 /** Intento enviado al proveedor que nunca recibió respuesta (timeout), con su antigüedad en segundos. */
 public record Atascado(String clave,long segundos) {}
 @Transactional(propagation=Propagation.REQUIRES_NEW,readOnly=true)
 public java.util.Optional<Atascado> sinResolver(String referencia){
  return jdbc.query("SELECT clave, CAST(EXTRACT(EPOCH FROM (CURRENT_TIMESTAMP - actualizado)) AS BIGINT) FROM intentos_checkout_interno WHERE referencia=? AND estado='ENVIANDO' AND payment_id IS NULL",
   (r,n)->new Atascado(r.getString(1),r.getLong(2)),referencia).stream().findFirst();
 }
 @Transactional(propagation=Propagation.REQUIRES_NEW)
 public void guardar(String referencia,String clave,String estado,String paymentId){jdbc.update("UPDATE intentos_checkout_interno SET estado=?,payment_id=?,actualizado=CURRENT_TIMESTAMP WHERE referencia=? AND clave=?",estado,paymentId,referencia,clave);}
}
