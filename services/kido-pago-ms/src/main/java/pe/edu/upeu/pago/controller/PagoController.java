package pe.edu.upeu.pago.controller;
import jakarta.validation.Valid; import lombok.RequiredArgsConstructor; import org.springframework.http.*; import org.springframework.web.bind.annotation.*; import pe.edu.upeu.pago.dto.*; import pe.edu.upeu.pago.service.PagoService; import java.util.*;
@RestController @RequestMapping("/api/v1/pagos") @RequiredArgsConstructor public class PagoController {private final PagoService service; private final pe.edu.upeu.pago.service.CertificadoPagoService certificadoService;
 @GetMapping("/ordenes") public List<OrdenResponse> listar(){return service.listar();}
 @GetMapping("/ordenes/{id}") public OrdenResponse obtener(@PathVariable Long id){return service.obtener(id);}
 @GetMapping("/ordenes/docente/{id}") public List<OrdenResponse> docente(@PathVariable Long id){return service.porDocente(id);}
 @GetMapping("/ordenes/estudiante/{id}") public List<OrdenResponse> estudiante(@PathVariable Long id){return service.porEstudiante(id);}
 @PostMapping("/ordenes") public ResponseEntity<OrdenResponse> crear(@Valid @RequestBody CrearOrdenRequest r){return ResponseEntity.status(201).body(service.crear(r));}
 @PostMapping("/ordenes/{id}/mercado-pago") public PreferenciaResponse iniciar(@PathVariable Long id){return service.iniciarMercadoPago(id);}
 @PostMapping("/ordenes/{id}/confirmar-mercado-pago") public OrdenResponse confirmar(@PathVariable Long id,@RequestParam String paymentId){return service.confirmarMercadoPago(id,paymentId);}
 @PostMapping("/ordenes/{id}/simular-aprobacion") public OrdenResponse simular(@PathVariable Long id){return service.simularAprobacion(id);}
 @PostMapping("/ordenes/{id}/reintentar-inscripcion") public OrdenResponse reintentar(@PathVariable Long id){return service.reintentarInscripcion(id);}
 @GetMapping("/docentes/{id}/saldo") public SaldoResponse saldo(@PathVariable Long id){return service.saldo(id);}
 @PostMapping("/saldos/liberar") public void liberar(){service.liberarSaldosVencidos();}
 @PostMapping("/certificados/ordenes") public ResponseEntity<OrdenCertificadoResponse> crearCert(@Valid @RequestBody CrearOrdenCertificadoRequest r){return ResponseEntity.status(201).body(certificadoService.crear(r));}
 @PostMapping("/certificados/ordenes/{id}/mercado-pago") public PreferenciaResponse iniciarCert(@PathVariable Long id){return certificadoService.iniciar(id);}
 @PostMapping("/certificados/ordenes/{id}/confirmar-mercado-pago") public OrdenCertificadoResponse confirmarCert(@PathVariable Long id,@RequestParam String paymentId){return certificadoService.confirmar(id,paymentId);}
 @PostMapping("/certificados/ordenes/{id}/simular-aprobacion") public OrdenCertificadoResponse simCert(@PathVariable Long id){return certificadoService.simular(id);}
 @GetMapping("/certificados/ordenes/estudiante/{id}") public List<OrdenCertificadoResponse> certEst(@PathVariable Long id){return certificadoService.estudiante(id);}
 @PostMapping("/webhooks/mercado-pago") public ResponseEntity<Void> webhook(@RequestParam(required=false) String type,@RequestParam(name="data.id",required=false) String dataId,@RequestBody(required=false) Map<String,Object> body){String id=dataId;if(id==null&&body!=null&&body.get("data") instanceof Map<?,?> data&&data.get("id")!=null)id=String.valueOf(data.get("id"));if(id!=null)service.procesarWebhook(id);return ResponseEntity.ok().build();}
}