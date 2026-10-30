package pe.edu.upeu.pago.controller;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import pe.edu.upeu.pago.service.*;
@RestController @RequestMapping("/api/v1/pagos/carrito") @RequiredArgsConstructor
public class CheckoutCarritoController {
 private final CheckoutCarritoService service;private final AccesoCuentaPago acceso;
 @PostMapping("/checkout") public CheckoutCarritoService.Resultado iniciar(@RequestBody CheckoutCarritoService.Solicitud req,Authentication auth){return service.iniciar(acceso.estudianteActual(),auth.getName(),req);}
 @GetMapping("/compras/{id}") public CheckoutCarritoService.Resultado obtener(@PathVariable Long id){acceso.verificar(service.propietario(id));return service.obtener(id);}
 @PostMapping("/compras/{id}/verificar") public CheckoutCarritoService.Resultado verificar(@PathVariable Long id){acceso.verificar(service.propietario(id));return service.verificar(id);}
 @PostMapping("/compras/{id}/confirmar") public CheckoutCarritoService.Resultado confirmar(@PathVariable Long id,@RequestParam String paymentId){acceso.verificar(service.propietario(id));return service.confirmar(id,paymentId);}
}
