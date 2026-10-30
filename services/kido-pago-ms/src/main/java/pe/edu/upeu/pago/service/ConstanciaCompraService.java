package pe.edu.upeu.pago.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.pago.client.MercadoPagoClient;
import pe.edu.upeu.pago.entity.*;
import pe.edu.upeu.pago.repository.*;
import pe.edu.upeu.pago.exception.ResourceNotFoundException;
import java.time.*;
import java.util.Objects;

@Service @RequiredArgsConstructor
public class ConstanciaCompraService {
 private final OrdenCompraRepository ordenes;
 private final ConstanciaCompraRepository constancias;
 private final ConfiguracionComercialRepository comercio;
 private final MercadoPagoClient mp;
 private final AccesoCuentaPago acceso;
 private final PagoCarritoValidador validadorCarrito;
 private final CompraCarritoRepository carritos;

 public record Operacion(String id,String fecha,String medio,String estado,String detalle,java.math.BigDecimal total) {}
 @Transactional(readOnly=true) public java.util.List<Operacion> operaciones(Long id){
   var o=ordenes.findById(id).orElseThrow(()->new ResourceNotFoundException("Orden no encontrada"));acceso.verificar(o.getEstudianteId());
   if(o.getAmbiente()==AmbientePago.DESCONOCIDO||o.getReceptorMp()==null)throw new IllegalStateException("El ambiente de la orden no está verificado");
   var resultado=new java.util.ArrayList<Operacion>();
   for(String op:mp.pagosAprobadosDeOrden(o.getCompraCarritoId()==null?String.valueOf(id):"CARRITO-"+o.getCompraCarritoId())){var p=mp.obtenerPago(op);validar(o,p);resultado.add(new Operacion(p.id(),p.fechaAprobada(),p.medioPago(),p.status(),p.detalleEstado(),o.getCompraCarritoId()==null?o.getMontoTotal():carritos.findById(o.getCompraCarritoId()).orElseThrow().getTotal()));}
   return java.util.List.copyOf(resultado);
 }
 @Transactional public ConstanciaCompra obtener(Long id){return obtener(id,null);}
 @Transactional public ConstanciaCompra obtener(Long id,String operacion){
   var inicial=ordenes.findById(id).orElseThrow(()->new ResourceNotFoundException("Orden no encontrada"));
   acceso.verificar(inicial.getEstudianteId());
   Long canon=inicial.getCompraCarritoId()==null?id:ordenes.findByCompraCarritoIdOrderByIdAsc(inicial.getCompraCarritoId()).getFirst().getId();
   var o=ordenes.bloquear(canon).orElseThrow();
   if(!Objects.equals(o.getEstudianteId(),inicial.getEstudianteId()))throw new IllegalStateException("El titular del detalle no coincide con la compra conjunta");
   if(o.getEstado()!=OrdenCompra.EstadoOrden.APROBADA||o.getMercadoPagoPaymentId()==null||o.getMercadoPagoPaymentId().startsWith("SIM-")||o.getAmbiente()==AmbientePago.DESCONOCIDO)
     throw new IllegalStateException("La constancia estará disponible después de verificar el pago de esta orden");
   String elegida=operacion==null?o.getMercadoPagoPaymentId():operacion;
   if(!elegida.matches("[0-9]{1,30}"))throw new IllegalArgumentException("Operación inválida");
   var existente=constancias.findById(elegida);if(existente.isPresent()){if(!canon.equals(existente.get().getOrdenId()))throw new IllegalStateException("La operación pertenece a otra orden");return existente.get();}
   var p=mp.obtenerPago(elegida);
   if(!elegida.equals(p.id()))throw new IllegalStateException("La operación no corresponde a la consulta");
   validar(o,p);
   if(p.medioPago()==null||p.detalleEstado()==null||p.fechaAprobada()==null)throw new IllegalStateException("Mercado Pago no devolvió todos los datos de la operación");
   var persona=acceso.datosComprador(o.getEstudianteId());var c=new ConstanciaCompra();
   c.setOrdenId(canon);c.setNumero("KIDO-"+canon+"-"+p.id());c.setOperacion(p.id());c.setAmbiente(o.getAmbiente().name());
   c.setAlumnoNombre(persona.nombre());c.setAlumnoEmail(persona.email());c.setAlumnoDocumento(persona.documento());
   c.setCursoTitulo(o.getCursoTitulo());c.setDocenteId(o.getDocenteId());c.setTotal(o.getMontoTotal());
   if(o.getCompraCarritoId()!=null){var grupo=carritos.findById(o.getCompraCarritoId()).orElseThrow();c.setCompraCarritoId(grupo.getId());c.setNumero("KIDO-CARRITO-"+grupo.getId()+"-"+p.id());c.setCursoTitulo("Compra de "+ordenes.findByCompraCarritoIdOrderByIdAsc(grupo.getId()).size()+" cursos");c.setTotal(grupo.getTotal());for(var linea:ordenes.findByCompraCarritoIdOrderByIdAsc(grupo.getId()))c.getDetalles().add(new DetalleConstancia(linea.getId(),linea.getCursoId(),linea.getDocenteId(),linea.getCursoTitulo(),linea.getMontoTotal()));}
   c.setFechaPago(OffsetDateTime.parse(p.fechaAprobada()).atZoneSameInstant(ZoneId.of("America/Lima")).toLocalDateTime());
   c.setMedioPago(p.medioPago());c.setDetalleEstado(p.detalleEstado());
   c.setSoporte(comercio.findById(1L).map(ConfiguracionComercial::getEmailSoporte).orElse(""));
   c.setGeneradaEn(LocalDateTime.now(ZoneId.of("America/Lima")));return constancias.save(c);
 }
 private void validar(OrdenCompra o,MercadoPagoClient.PagoExterno p){if(!"approved".equals(p.status()))throw new IllegalStateException("El pago no está acreditado");if(o.getCompraCarritoId()!=null){validadorCarrito.paraLinea(o,p);return;}if(!String.valueOf(o.getId()).equals(p.externalReference()))throw new IllegalStateException("El pago no está acreditado para esta orden");p.validarImporte(o.getMontoTotal());mp.validarPago(p,o.getReceptorMp(),o.getAmbiente());}
}
