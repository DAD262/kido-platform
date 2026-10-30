package pe.edu.upeu.pago.controller;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import pe.edu.upeu.pago.entity.ConstanciaCompra;
import pe.edu.upeu.pago.service.*;

@RestController @RequestMapping("/api/v1/pagos/ordenes") @RequiredArgsConstructor
public class ConstanciaCompraController {
 private final ConstanciaCompraService service;
 private final ConstanciaPdfService pdf;
 @GetMapping("/{id}/operaciones") public ResponseEntity<java.util.List<ConstanciaCompraService.Operacion>> operaciones(@PathVariable Long id){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.operaciones(id));}
 @GetMapping("/{id}/constancia") public ResponseEntity<ConstanciaCompra> datos(@PathVariable Long id,@RequestParam(required=false) String operacion){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.obtener(id,operacion));}
 @GetMapping(value="/{id}/constancia.pdf",produces=MediaType.APPLICATION_PDF_VALUE)
 public ResponseEntity<byte[]> descargar(@PathVariable Long id,@RequestParam(required=false) String operacion){var c=service.obtener(id,operacion);return ResponseEntity.ok().cacheControl(CacheControl.noStore()).header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=constancia-kido-"+id+"-"+c.getOperacion()+".pdf").body(pdf.generar(c));}
}
