package pe.edu.upeu.pago.controller;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import pe.edu.upeu.pago.service.CheckoutInternoService;
@RestController @RequestMapping("/api/v1/pagos/checkout-interno") @RequiredArgsConstructor
public class CheckoutInternoController {
 private final CheckoutInternoService service;
 @GetMapping("/{tipo}/{id}") public CheckoutInternoService.Configuracion configurar(@PathVariable String tipo,@PathVariable Long id){return service.configurar(tipo,id);}
 @PostMapping("/{tipo}/{id}") public CheckoutInternoService.Resultado pagar(@PathVariable String tipo,@PathVariable Long id,@RequestBody CheckoutInternoService.Solicitud req,Authentication auth){return service.pagar(tipo,id,auth.getName(),req);}
}
