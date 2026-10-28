package pe.edu.upeu.pago.controller;
import jakarta.validation.Valid; import lombok.RequiredArgsConstructor; import org.springframework.http.*; import org.springframework.web.bind.annotation.*; import pe.edu.upeu.pago.dto.*; import pe.edu.upeu.pago.service.PagoService; import java.util.*;
@RestController @RequestMapping("/api/v1/pagos") @RequiredArgsConstructor public class PagoController {private final PagoService service; private final pe.edu.upeu.pago.webhook.ValidadorWebhookMercadoPago validadorWebhook; private static final org.slf4j.Logger webhookLog = org.slf4j.LoggerFactory.getLogger(PagoController.class); private final pe.edu.upeu.pago.service.CertificadoPagoService certificadoService;
 private final pe.edu.upeu.pago.client.MercadoPagoClient mercadoPago;
 private final pe.edu.upeu.pago.service.AccesoCuentaPago acceso;
 private final pe.edu.upeu.pago.service.ComercioService comercio;
 private final pe.edu.upeu.pago.service.CheckoutService checkout;
 private void verificarOrden(Long id){acceso.verificar(service.obtener(id).estudianteId());}
 private void verificarCertificado(Long id){acceso.verificar(certificadoService.obtener(id).estudianteId());}
 @GetMapping("/metodos") public List<MetodoPagoResponse> metodos(){return checkout.metodos();}
 @GetMapping("/ordenes") public List<OrdenResponse> listar(){return service.listar();}
 @GetMapping("/ordenes/{id}") public OrdenResponse obtener(@PathVariable Long id){verificarOrden(id);return service.obtener(id);}
 @GetMapping("/ordenes/docente/{id}") public List<OrdenResponse> docente(@PathVariable Long id){acceso.verificar(id);return service.porDocente(id);}
 @GetMapping("/ordenes/estudiante/{id}") public List<OrdenResponse> estudiante(@PathVariable Long id){acceso.verificar(id);return service.porEstudiante(id);}
 @PostMapping("/ordenes") public ResponseEntity<OrdenResponse> crear(@Valid @RequestBody CrearOrdenRequest r){acceso.verificar(r.estudianteId());return ResponseEntity.status(201).body(service.crear(r));}
 @PostMapping("/ordenes/{id}/mercado-pago") public PreferenciaResponse iniciar(@PathVariable Long id,@RequestBody(required=false) AceptacionRequest aceptacion,org.springframework.security.core.Authentication auth){verificarOrden(id);checkout.exigirActivacion();comercio.aceptar("CURSO",id,service.obtener(id).estudianteId(),auth.getName(),aceptacion);return service.iniciarMercadoPago(id);}
 @PostMapping("/ordenes/{id}/confirmar-mercado-pago") public OrdenResponse confirmar(@PathVariable Long id,@RequestParam String paymentId){verificarOrden(id);return service.confirmarMercadoPago(id,paymentId);}
 @PostMapping("/ordenes/{id}/simular-aprobacion") public OrdenResponse simular(@PathVariable Long id){return service.simularAprobacion(id);}
 @PostMapping("/ordenes/{id}/reintentar-inscripcion") public OrdenResponse reintentar(@PathVariable Long id){return service.reintentarInscripcion(id);} @PostMapping("/ordenes/{id}/republicar-evento") public OrdenResponse republicar(@PathVariable Long id){return service.republicarEventoPago(id);}
 @GetMapping("/docentes/{id}/saldo") public SaldoResponse saldo(@PathVariable Long id){acceso.verificar(id);return service.saldo(id);}
 @PostMapping("/saldos/liberar") public void liberar(){service.liberarSaldosVencidos();}
 @PostMapping("/certificados/ordenes") public ResponseEntity<OrdenCertificadoResponse> crearCert(@Valid @RequestBody CrearOrdenCertificadoRequest r){acceso.verificar(r.estudianteId());return ResponseEntity.status(201).body(certificadoService.crear(r));}
 @PostMapping("/certificados/ordenes/{id}/mercado-pago") public PreferenciaResponse iniciarCert(@PathVariable Long id,@RequestBody(required=false) AceptacionRequest aceptacion,org.springframework.security.core.Authentication auth){verificarCertificado(id);checkout.exigirActivacion();comercio.aceptar("CERTIFICADO",id,certificadoService.obtener(id).estudianteId(),auth.getName(),aceptacion);return certificadoService.iniciar(id);}
 @PostMapping("/certificados/ordenes/{id}/confirmar-mercado-pago") public OrdenCertificadoResponse confirmarCert(@PathVariable Long id,@RequestParam String paymentId){verificarCertificado(id);return certificadoService.confirmar(id,paymentId);}
 @PostMapping("/certificados/ordenes/{id}/simular-aprobacion") public OrdenCertificadoResponse simCert(@PathVariable Long id){return certificadoService.simular(id);}
 @GetMapping("/certificados/ordenes/estudiante/{id}") public List<OrdenCertificadoResponse> certEst(@PathVariable Long id){acceso.verificar(id);return certificadoService.estudiante(id);}
 /**
  * Webhook de Mercado Pago. Solo procesa el tópico "payment"; otros tópicos (merchant_order, etc.)
  * se confirman con 200 para que Mercado Pago no los reintente. Con MERCADO_PAGO_WEBHOOK_SECRET
  * configurado, una firma x-signature inválida responde 401.
  */
 @PostMapping("/webhooks/mercado-pago")
 public ResponseEntity<Void> webhook(
     @RequestParam(required=false) String type,
     @RequestParam(required=false) String topic,
     @RequestParam(name="data.id",required=false) String dataId,
     @RequestParam(required=false) String id,
     @RequestHeader(name="x-signature",required=false) String firma,
     @RequestHeader(name="x-request-id",required=false) String requestId,
     @RequestBody(required=false) Map<String,Object> body){
   String tipo = type != null ? type : topic;
   if (tipo == null && body != null) tipo = body.get("type") != null ? String.valueOf(body.get("type")) : body.get("topic") != null ? String.valueOf(body.get("topic")) : null;
   String pagoId = dataId;
   if (pagoId == null && body != null && body.get("data") instanceof Map<?,?> data && data.get("id") != null) pagoId = String.valueOf(data.get("id"));
   if (pagoId == null && "payment".equals(tipo)) pagoId = id; // formato IPN antiguo: ?topic=payment&id=123
   if (validadorWebhook.activo()) {
     if (firma == null) return ResponseEntity.status(401).build();
     if (!validadorWebhook.aceptar(firma, requestId, dataId != null ? dataId : pagoId)) return ResponseEntity.status(401).build();
   }
   if (!"payment".equals(tipo) || pagoId == null) return ResponseEntity.ok().build();
   try {
     service.procesarWebhook(pagoId);
   } catch (IllegalArgumentException | IllegalStateException e) {
     // Error de negocio (monto distinto, orden cerrada, referencia ajena): reintentar no lo arregla.
     webhookLog.warn("Webhook de pago {} no aplicado: {}", pagoId, e.getMessage());
   }
   return ResponseEntity.ok().build();
 }
}
