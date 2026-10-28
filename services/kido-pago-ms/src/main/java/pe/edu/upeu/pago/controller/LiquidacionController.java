package pe.edu.upeu.pago.controller;
import lombok.RequiredArgsConstructor;import org.springframework.web.bind.annotation.*;import org.springframework.http.*;import org.springframework.web.multipart.MultipartFile;import org.springframework.security.core.Authentication;import jakarta.validation.Valid;
import pe.edu.upeu.pago.dto.*;import pe.edu.upeu.pago.service.*;import java.time.LocalDate;import java.util.*;import java.io.IOException;
@RestController @RequestMapping("/api/v1/pagos/liquidaciones") @RequiredArgsConstructor
public class LiquidacionController {
 @org.springframework.beans.factory.annotation.Value("${kido.pagos.comision:0.20}") private java.math.BigDecimal comision;
 private final LiquidacionService service;private final AccesoCuentaPago acceso;
 public record GenerarRequest(LocalDate fechaCorte){}
 @GetMapping("/politica") public Map<String,Object> politica(){return Map.of("comisionKido",comision.multiply(new java.math.BigDecimal("100")),"participacionDocente",java.math.BigDecimal.ONE.subtract(comision).multiply(new java.math.BigDecimal("100")),"periodicidad","QUINCENAL","ultimoCorte",service.ultimoCorte(),"transferenciaAutomatica",false,"costosProveedor","A cargo de KIDO; no reducen el 80 % docente");}
 @PostMapping("/generar") public LiquidacionService.Generacion generar(@RequestBody(required=false) GenerarRequest r){return service.generar(r==null?null:r.fechaCorte());}
 @GetMapping public List<LiquidacionResponse> listar(){return service.listar();}
 @GetMapping("/docente/{id}") public List<LiquidacionResponse> docente(@PathVariable Long id){acceso.verificar(id);return service.docente(id);}
 private void verificar(Long id){acceso.verificar(service.obtener(id).docenteId());}
 @GetMapping("/{id}") public LiquidacionResponse obtener(@PathVariable Long id){verificar(id);return service.obtener(id);}
 @GetMapping("/{id}/detalle") public java.util.List<pe.edu.upeu.pago.entity.LiquidacionDetalle> detalle(@PathVariable Long id){verificar(id);return service.detalle(id);}
 @PostMapping(value="/{id}/pagar",consumes=MediaType.MULTIPART_FORM_DATA_VALUE) public LiquidacionResponse pagar(@PathVariable Long id,@Valid @RequestPart("datos") PagoLiquidacionRequest datos,@RequestPart("constancia") MultipartFile constancia,Authentication auth)throws IOException{return service.pagar(id,datos,constancia.getBytes(),constancia.getContentType(),auth.getName());}
 @PostMapping("/{id}/anular") public LiquidacionResponse anular(@PathVariable Long id,@Valid @RequestBody RechazoRequest r){return service.anular(id,r.motivo());}
 @GetMapping("/{id}/constancia") public ResponseEntity<byte[]> constancia(@PathVariable Long id){verificar(id);var c=service.constancia(id);String extension="application/pdf".equals(c.tipo())?"pdf":"image/png".equals(c.tipo())?"png":"jpg";return ResponseEntity.ok().contentType(MediaType.parseMediaType(c.tipo())).header("X-Content-Type-Options","nosniff").header("Content-Disposition","attachment; filename=liquidacion-"+id+"."+extension).body(c.bytes());}
}
