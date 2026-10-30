package pe.edu.upeu.pago.service;

import jakarta.persistence.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.pago.client.CursoClient;
import pe.edu.upeu.pago.dto.*;
import pe.edu.upeu.pago.entity.*;
import pe.edu.upeu.pago.repository.OrdenCompraRepository;
import java.math.BigDecimal;
import java.util.*;

/** Un acceso digital por curso; importes siempre obtenidos del catálogo del backend. */
@Service @RequiredArgsConstructor
public class CarritoService {
 private final EntityManager em;
 private final CursoClient cursos;
 private final OrdenCompraRepository ordenes;
 private final PagoService pagos;
 public record Item(Long cursoId,String titulo,BigDecimal precio,boolean disponible,String motivo) {}
 public record Resumen(List<Item> items,BigDecimal total,String moneda,List<Long> comprados) {}
 public record Comprar(Long cursoId,BigDecimal precioAceptado) {}
 @Transactional public List<CursoCompraDto> validarTodos(Long estudiante,List<Long> aceptados,BigDecimal totalAceptado){
  var carrito=bloquear(estudiante);
  if(carrito.getCursos().isEmpty()||aceptados==null||aceptados.size()!=new HashSet<>(aceptados).size()||!carrito.getCursos().equals(new HashSet<>(aceptados)))throw new IllegalStateException("El carrito cambió. Actualízalo antes de pagar");
  var lista=new ArrayList<CursoCompraDto>();
  for(Long id:carrito.getCursos().stream().sorted().toList()){var item=evaluar(estudiante,id);if(!item.disponible())throw new IllegalStateException(item.titulo()+": "+item.motivo());lista.add(cursos.obtener(id));}
  var total=lista.stream().map(CursoCompraDto::precio).reduce(BigDecimal.ZERO,BigDecimal::add);
  if(totalAceptado==null||total.compareTo(totalAceptado)!=0)throw new IllegalStateException("Los precios cambiaron. Revisa el total actualizado");
  return List.copyOf(lista);
 }
 private CarritoEstudiante bloquear(Long estudiante) {
  em.createNativeQuery("INSERT INTO carritos_estudiante(estudiante_id,version) VALUES (:id,0) ON CONFLICT DO NOTHING").setParameter("id",estudiante).executeUpdate();
  return em.find(CarritoEstudiante.class,estudiante,LockModeType.PESSIMISTIC_WRITE);
 }
 private boolean comprado(Long estudiante,Long curso) {
  return ordenes.existsByEstudianteIdAndCursoIdAndEstado(estudiante,curso,OrdenCompra.EstadoOrden.APROBADA);
 }
 private Item evaluar(Long estudiante,Long id) {
  CursoCompraDto c;
  try {c=cursos.obtener(id);}catch(pe.edu.upeu.pago.exception.ResourceNotFoundException e){return new Item(id,"Curso no disponible",BigDecimal.ZERO,false,"Curso retirado");}
  if(c==null) return new Item(id,"Curso no disponible",BigDecimal.ZERO,false,"Curso retirado");
  String motivo=comprado(estudiante,id)?"Ya compraste este curso":!"PUBLICADO".equals(c.estado())?"Curso no publicado":!"PAGO".equals(c.tipo())?"Este curso es gratuito; inscríbete desde su página":c.precio()==null||c.precio().signum()<=0?"Precio no disponible":"";
  return new Item(id,c.titulo(),c.precio()==null?BigDecimal.ZERO:c.precio(),motivo.isEmpty(),motivo);
 }
 private Resumen resumir(Long estudiante,CarritoEstudiante carrito) {
  var comprados=ordenes.findByEstudianteIdOrderByIdDesc(estudiante).stream().filter(o->o.getEstado()==OrdenCompra.EstadoOrden.APROBADA).map(OrdenCompra::getCursoId).distinct().toList();
  carrito.getCursos().removeAll(comprados);
  var items=carrito.getCursos().stream().sorted().map(id->evaluar(estudiante,id)).toList();
  return new Resumen(items,items.stream().filter(Item::disponible).map(Item::precio).reduce(BigDecimal.ZERO,BigDecimal::add),"PEN",comprados);
 }
 @Transactional public Resumen obtener(Long estudiante){return resumir(estudiante,bloquear(estudiante));}
 @Transactional public Resumen agregar(Long estudiante,Long curso) {
  if(curso==null||curso<=0)throw new IllegalArgumentException("Curso inválido");
  var carrito=bloquear(estudiante);
  var item=evaluar(estudiante,curso);
  if(!item.disponible())throw new IllegalStateException(item.motivo());
  if(!carrito.getCursos().contains(curso)&&carrito.getCursos().size()>=20)throw new IllegalStateException("El carrito admite hasta 20 cursos");
  carrito.getCursos().add(curso);return resumir(estudiante,carrito);
 }
 @Transactional public Resumen quitar(Long estudiante,Long curso){var c=bloquear(estudiante);c.getCursos().remove(curso);return resumir(estudiante,c);}
 @Transactional public Resumen vaciar(Long estudiante){var c=bloquear(estudiante);c.getCursos().clear();return resumir(estudiante,c);}
 @Transactional public OrdenResponse preparar(Long estudiante,Comprar req) {
  var c=bloquear(estudiante);
  if(req==null||req.cursoId()==null||!c.getCursos().contains(req.cursoId()))throw new IllegalArgumentException("El curso no está en tu carrito");
  var item=evaluar(estudiante,req.cursoId());
  if(!item.disponible())throw new IllegalStateException(item.motivo());
  if(req.precioAceptado()==null||item.precio().compareTo(req.precioAceptado())!=0)throw new IllegalStateException("El precio cambió. Actualiza el carrito y revisa el importe antes de continuar");
  var orden=pagos.crear(new CrearOrdenRequest(estudiante,req.cursoId()));
  if(orden.montoTotal().compareTo(item.precio())!=0)throw new IllegalStateException("La orden pendiente tiene otro precio y requiere revisión");
  return orden;
 }
}
