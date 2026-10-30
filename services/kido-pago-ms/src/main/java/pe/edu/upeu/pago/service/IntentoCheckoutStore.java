package pe.edu.upeu.pago.service;

import java.util.*;
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
  if(!i.clave().equals(clave)||!i.huella().equals(huella))throw new IllegalStateException("Existe un intento sin resolver. Verifica tu compra antes de iniciar otro pago.");
  return i;
 }
 @Transactional(propagation=Propagation.REQUIRES_NEW)
 public void guardar(String referencia,String clave,String estado,String paymentId){jdbc.update("UPDATE intentos_checkout_interno SET estado=?,payment_id=?,actualizado=CURRENT_TIMESTAMP WHERE referencia=? AND clave=?",estado,paymentId,referencia,clave);}
}
