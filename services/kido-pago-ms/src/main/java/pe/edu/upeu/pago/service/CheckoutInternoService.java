package pe.edu.upeu.pago.service;

import java.math.BigDecimal;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import pe.edu.upeu.pago.client.MercadoPagoClient;
import pe.edu.upeu.pago.dto.*;
import pe.edu.upeu.pago.repository.*;

@Service @RequiredArgsConstructor
public class CheckoutInternoService {
 private final PagoService pagos;private final CertificadoPagoService certificados;private final CheckoutCarritoService carritos;
 private final CheckoutService checkout;private final ComercioService comercio;private final AccesoCuentaPago acceso;private final MercadoPagoClient mp;private final IntentoCheckoutStore intentos;
 private final OrdenCompraRepository ordenes;private final OrdenCertificadoRepository ordenesCertificado;private final CompraCarritoRepository compras;
 @Value("${MERCADO_PAGO_PUBLIC_KEY:}") private String publicKey;
 public record Documento(String type,String number){}
 public record Pagador(String email,String first_name,String last_name,Documento identification){}
 public record Solicitud(String solicitudId,String payment_method_id,String token,Integer installments,String issuer_id,Pagador payer,AceptacionRequest aceptacion){}
 public record Resultado(String id,String estado,String detalle,String codigo,String instrucciones,boolean confirmado){}
 public record Configuracion(String publicKey,boolean configurado,BigDecimal total,String ambiente,List<MetodoPagoResponse.Medio> medios,String email,long version){}
 record Destino(Long propietario,BigDecimal total,String estado,String ambiente,String referencia,String tipoComercial){}
 private Destino destino(String tipo,Long id){
  if(id==null||id<=0)throw new IllegalArgumentException("Compra inválida");
  Destino d=switch(tipo){
   case "curso"->{var o=pagos.obtener(id);if(o.compraCarritoId()!=null)throw new IllegalArgumentException("Usa la compra conjunta");yield new Destino(o.estudianteId(),o.montoTotal(),o.estado(),o.ambiente(),""+id,"CURSO");}
   case "certificado"->{var o=certificados.obtener(id);yield new Destino(o.estudianteId(),o.montoTotal(),o.estado(),o.ambiente(),"CERT-"+id,"CERTIFICADO");}
   case "carrito"->{acceso.verificar(carritos.propietario(id));var c=carritos.obtener(id);yield new Destino(carritos.propietario(id),c.total(),c.estado(),c.ambiente(),"CARRITO-"+id,"CARRITO");}
   default->throw new IllegalArgumentException("Tipo de compra inválido");
  };
  acceso.verificar(d.propietario());return d;
 }
 public Configuracion configurar(String tipo,Long id){var d=destino(tipo,id);var persona=acceso.datosComprador(d.propietario());return new Configuracion(publicKey,!publicKey.isBlank(),d.total(),d.ambiente(),mp.mediosDisponibles(),persona.email(),comercio.obtener(true).datos().version());}
 public Resultado pagar(String tipo,Long id,String sujeto,Solicitud r){
  var d=destino(tipo,id);checkout.exigirActivacion();
  if(publicKey.isBlank())throw new IllegalStateException("El formulario interno está pendiente de configurar la Public Key de Mercado Pago.");
  if(!Set.of("PENDIENTE","PAGO_INICIADO").contains(d.estado()))throw new IllegalStateException("Esta compra no admite un nuevo pago");
  if(r==null||r.solicitudId()==null)throw new IllegalArgumentException("Falta el identificador del intento");
  UUID.fromString(r.solicitudId());
  var cuenta=mp.verificarCuentaConfigurada();if(!cuenta.ambiente().name().equals(d.ambiente()))throw new IllegalStateException("El ambiente de la compra no coincide");
  String receptor=switch(tipo){case "curso"->ordenes.findById(id).orElseThrow().getReceptorMp();case "certificado"->ordenesCertificado.findById(id).orElseThrow().getReceptorMp();case "carrito"->compras.findById(id).orElseThrow().getReceptorMp();default->throw new IllegalArgumentException("Tipo inválido");};
  if(!Objects.equals(receptor,cuenta.receptor()))throw new IllegalStateException("El receptor de la compra no coincide con la cuenta configurada");
  var medio=mp.mediosDisponibles().stream().filter(m->m.id().equals(r.payment_method_id())).findFirst().orElseThrow(()->new IllegalArgumentException("Medio de pago no disponible"));
  boolean efectivo="pagoefectivo_atm".equals(medio.id());
  if(!efectivo&&(r.token()==null||!r.token().matches("[A-Za-z0-9_-]{8,256}")))throw new IllegalArgumentException("Falta el token seguro del medio de pago");
  if(r.installments()==null||r.installments()<1||r.installments()>36)throw new IllegalArgumentException("Cuotas inválidas");
  if((efectivo||"yape".equals(medio.id()))&&r.installments()!=1)throw new IllegalArgumentException("Este medio requiere un solo pago");
  if(r.payer()==null||r.payer().email()==null||!r.payer().email().matches("[^\\s@]{1,100}@[^\\s@]{1,100}\\.[^\\s@]{2,20}"))throw new IllegalArgumentException("Correo de pago inválido");
  Map<String,Object> pagador=new LinkedHashMap<>();pagador.put("email",r.payer().email());
  if(r.payer().identification()!=null){var doc=r.payer().identification();if(!Set.of("DNI","CE","RUC","PAS").contains(doc.type())||doc.number()==null||!doc.number().matches("[A-Za-z0-9]{6,20}"))throw new IllegalArgumentException("Documento inválido");pagador.put("identification",Map.of("type",doc.type(),"number",doc.number()));}
  if(efectivo){if(r.payer().identification()==null)throw new IllegalArgumentException("PagoEfectivo requiere documento");for(var e:Map.of("first_name",Objects.toString(r.payer().first_name(),""),"last_name",Objects.toString(r.payer().last_name(),"")).entrySet()){if(e.getValue().isBlank()||e.getValue().length()>100)throw new IllegalArgumentException("Completa los nombres y apellidos");pagador.put(e.getKey(),e.getValue());}}
  if(checkout.excepcionLocal()&&!comercio.obtener(true).lista())comercio.aceptarPrueba(d.tipoComercial(),id,d.propietario(),sujeto,r.aceptacion());else comercio.aceptar(d.tipoComercial(),id,d.propietario(),sujeto,r.aceptacion());
  Map<String,Object> body=new LinkedHashMap<>();body.put("transaction_amount",d.total());body.put("description","Compra KIDO "+d.referencia());body.put("external_reference",d.referencia());body.put("payment_method_id",medio.id());body.put("installments",r.installments());body.put("payer",pagador);if(!efectivo)body.put("token",r.token());
  if(r.issuer_id()!=null&&!r.issuer_id().isBlank()){if(!r.issuer_id().matches("[0-9]{1,20}"))throw new IllegalArgumentException("Emisor inválido");body.put("issuer_id",r.issuer_id());}
  String huella;try{huella=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(body.toString().getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException("No se pudo preparar el intento");}
  var intento=intentos.iniciar(d.referencia(),r.solicitudId(),huella);
  Map<String,Object> res;
  try{res=intento.paymentId()!=null?mp.consultarPagoInterno(intento.paymentId()):mp.crearPagoInterno(body,intento.clave());}
  catch(org.springframework.web.client.RestClientResponseException e){
   // Solo una petición inválida con respuesta explícita permite un intento nuevo.
   // Un timeout o respuesta ambigua mantiene la clave para impedir un segundo cobro.
   if(intento.paymentId()==null&&(e.getStatusCode().value()==400||e.getStatusCode().value()==422)){
    intentos.guardar(d.referencia(),intento.clave(),"rejected",null);
    throw new IllegalArgumentException("El proveedor rechazó los datos de pago. Revisa el formulario y vuelve a intentarlo.");
   }
   throw e;
  }
  if(res==null)throw new IllegalStateException("El proveedor no respondió. Verifica tu compra antes de volver a pagar.");
  String pid=Objects.toString(res.get("id"),"");if(!pid.matches("[0-9]{1,40}"))throw new IllegalStateException("El proveedor no identificó la operación. Verifica antes de reintentar.");
  String estado=Objects.toString(res.get("status"),"pending");intentos.guardar(d.referencia(),intento.clave(),estado,pid);
  boolean confirmado=false;if("approved".equals(estado)){switch(tipo){case "curso"->pagos.confirmarMercadoPago(id,pid);case "certificado"->certificados.confirmar(id,pid);case "carrito"->carritos.confirmar(id,pid);}confirmado=true;}
  var detalle=res.get("transaction_details") instanceof Map<?,?> m?m:Map.of();
  String instrucciones=Objects.toString(detalle.get("external_resource_url"),"");if(!instrucciones.matches("https://([a-z0-9-]+\\.)*mercadopago\\.com(\\.[a-z]{2})?/.*"))instrucciones="";
  return new Resultado(pid,estado,Objects.toString(res.get("status_detail"),""),Objects.toString(detalle.get("payment_method_reference_id"),""),instrucciones,confirmado);
 }
}
