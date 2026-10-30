package pe.edu.upeu.pago.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import pe.edu.upeu.pago.service.*;
import pe.edu.upeu.pago.dto.OrdenResponse;

@RestController @RequestMapping("/api/v1/pagos/carrito") @RequiredArgsConstructor
public class CarritoController {
 private final CarritoService carrito;
 private final AccesoCuentaPago acceso;
 @GetMapping public CarritoService.Resumen obtener(){return carrito.obtener(acceso.estudianteActual());}
 @PutMapping("/{curso}") public CarritoService.Resumen agregar(@PathVariable Long curso){return carrito.agregar(acceso.estudianteActual(),curso);}
 @DeleteMapping("/{curso}") public CarritoService.Resumen quitar(@PathVariable Long curso){return carrito.quitar(acceso.estudianteActual(),curso);}
 @DeleteMapping public CarritoService.Resumen vaciar(){return carrito.vaciar(acceso.estudianteActual());}
 @PostMapping("/orden") public OrdenResponse preparar(@RequestBody CarritoService.Comprar req){return carrito.preparar(acceso.estudianteActual(),req);}
}
