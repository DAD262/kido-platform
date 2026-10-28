package pe.edu.upeu.pago.controller;
import lombok.RequiredArgsConstructor; import org.springframework.web.bind.annotation.*; import jakarta.validation.Valid;
import pe.edu.upeu.pago.dto.*; import pe.edu.upeu.pago.service.ComercioService;
@RestController @RequestMapping("/api/v1/pagos") @RequiredArgsConstructor
public class ComercioController {
 private final ComercioService comercio;
 private final pe.edu.upeu.pago.service.CheckoutService checkout;
 @GetMapping("/admin/activacion") public pe.edu.upeu.pago.service.CheckoutService.Activacion activacion(){return checkout.activacion();}
 @GetMapping("/informacion-comercial") public EstadoComercial publico(){return comercio.obtener(false);}
 @GetMapping("/admin/informacion-comercial") public EstadoComercial admin(){return comercio.obtener(true);}
 @PutMapping("/admin/informacion-comercial") public EstadoComercial guardar(@Valid @RequestBody DatosComerciales datos){return comercio.guardar(datos);}
}
