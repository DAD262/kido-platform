package pe.edu.upeu.pago.service;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pe.edu.upeu.pago.client.MercadoPagoClient;
import pe.edu.upeu.pago.entity.*;
import pe.edu.upeu.pago.repository.*;
import java.math.*;
import java.util.*;
@Component @RequiredArgsConstructor
public class PagoCarritoValidador {
 private final CompraCarritoRepository compras;private final OrdenCompraRepository ordenes;private final MercadoPagoClient mp;
 public void validar(CompraCarrito c,MercadoPagoClient.PagoExterno p){
  if(c.getAmbiente()==AmbientePago.DESCONOCIDO||!("CARRITO-"+c.getId()).equals(p.externalReference()))throw new IllegalArgumentException("El pago no corresponde al carrito");
  p.validarImporte(c.getTotal());mp.validarPago(p,c.getReceptorMp(),c.getAmbiente());
  var lineas=ordenes.findByCompraCarritoIdOrderByIdAsc(c.getId());
  if(lineas.isEmpty()||lineas.stream().anyMatch(o->!Objects.equals(o.getEstudianteId(),c.getEstudianteId())||o.getAmbiente()!=c.getAmbiente()||!Objects.equals(o.getReceptorMp(),c.getReceptorMp()))||lineas.stream().map(OrdenCompra::getMontoTotal).reduce(BigDecimal.ZERO,BigDecimal::add).compareTo(c.getTotal())!=0)throw new IllegalStateException("El detalle del carrito no coincide con el pago");
 }
 public MercadoPagoClient.PagoExterno paraLinea(OrdenCompra o,MercadoPagoClient.PagoExterno p){
  var c=compras.findById(o.getCompraCarritoId()).orElseThrow();validar(c,p);
  var lineas=ordenes.findByCompraCarritoIdOrderByIdAsc(c.getId());
  return new MercadoPagoClient.PagoExterno(p.id(),p.status(),p.externalReference(),o.getMontoTotal().toPlainString(),p.currency(),repartir(p.costoProcesamiento(),o,lineas,c.getTotal()),repartir(p.netoRecibido(),o,lineas,c.getTotal()),p.fondosEstado(),p.fechaLiberacion(),p.reembolsado(),p.receptor(),p.liveMode(),p.medioPago(),p.detalleEstado(),p.fechaAprobada());
 }
 /** Reparto en céntimos; el último acceso recibe el residuo, sin crear dinero. */
 public static BigDecimal repartir(BigDecimal valor,OrdenCompra o,List<OrdenCompra> lineas,BigDecimal total){
  if(valor==null)return null;
  BigDecimal acumulado=BigDecimal.ZERO;
  for(int i=0;i<lineas.size();i++){var linea=lineas.get(i);var parte=i==lineas.size()-1?valor.subtract(acumulado):valor.multiply(linea.getMontoTotal()).divide(total,2,RoundingMode.DOWN);if(Objects.equals(linea.getId(),o.getId()))return parte;acumulado=acumulado.add(parte);}
  throw new IllegalArgumentException("Acceso ajeno al carrito");
 }
}
