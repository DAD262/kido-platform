package pe.edu.upeu.pago.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.pago.dto.*;
import pe.edu.upeu.pago.entity.*;
import pe.edu.upeu.pago.repository.*;
import pe.edu.upeu.pago.exception.ResourceNotFoundException;
import java.time.*;
import java.math.BigDecimal;
import java.util.*;
import java.security.*;

@Service @RequiredArgsConstructor
public class LiquidacionService {
 private final LiquidacionRepository repo;
 private final LiquidacionDetalleRepository detalles;
 private final MovimientoSaldoRepository movimientos;
 private final BloqueoSaldoService bloqueo;
 private final Clock clock;
 private final ConstanciaLiquidacionRepository constancias;
 public LocalDate hoy(){return LocalDate.now(clock);}
 public LocalDate ultimoCorte(){return CalendarioLiquidaciones.ultimoCorte(hoy());}
 public record Generacion(int creadas,List<LiquidacionResponse> liquidaciones){}
 @Transactional public Generacion generar(LocalDate corte){
   if(corte==null)corte=ultimoCorte();
   CalendarioLiquidaciones.inicio(corte);
   if(corte.isAfter(ultimoCorte()))throw new IllegalArgumentException("Solo se liquidan periodos cerrados");
   final LocalDateTime fin=corteFinal(corte);
   var resultado=new ArrayList<LiquidacionResponse>();
   for(Long docente:movimientos.docentesConciliados().stream().sorted().toList()){
     bloqueo.bloquear(docente);
     if(repo.existsByDocenteIdAndCorteAndEstadoNot(docente,corte,LiquidacionDocente.Estado.ANULADA))continue;
     var libro=movimientos.findByDocenteId(docente);
     var elegibles=libro.stream().filter(m->elegible(m,fin)).toList();
     BigDecimal base=elegibles.stream().map(MovimientoSaldo::getMonto).reduce(BigDecimal.ZERO,BigDecimal::add);
     var comprometidos=libro.stream().filter(m->m.getAmbiente()==AmbientePago.PRODUCCION&&m.isConciliado()&&m.getMonto().signum()<0&&(m.getEstado()==MovimientoSaldo.EstadoMovimiento.RESERVADO||m.getEstado()==MovimientoSaldo.EstadoMovimiento.PAGADO)).toList();
     BigDecimal compromisos=comprometidos.stream().map(MovimientoSaldo::getMonto).reduce(BigDecimal.ZERO,BigDecimal::add).abs();
     BigDecimal monto=base.subtract(compromisos).setScale(2);
     if(monto.signum()<=0)continue;
     MovimientoSaldo reserva=new MovimientoSaldo();reserva.setAmbiente(AmbientePago.PRODUCCION);reserva.setDocenteId(docente);reserva.setMonto(monto.negate());reserva.setConciliado(true);reserva.setTipo(MovimientoSaldo.TipoMovimiento.LIQUIDACION);reserva.setEstado(MovimientoSaldo.EstadoMovimiento.RESERVADO);reserva.setFechaCreacion(LocalDateTime.now(clock));reserva.setDescripcion("Reserva de liquidación al "+corte);reserva=movimientos.save(reserva);
     var l=new LiquidacionDocente();l.setDocenteId(docente);l.setDesde(CalendarioLiquidaciones.inicio(corte));l.setCorte(corte);l.setEstado(LiquidacionDocente.Estado.PENDIENTE);l.setMonto(monto);l.setBaseAcumulada(base);l.setCompromisosAnteriores(compromisos);l.setMovimientoId(reserva.getId());l.setFechaCreacion(clock.instant());l=repo.saveAndFlush(l);
     reserva.setLiquidacionId(l.getId());movimientos.save(reserva);
     for(var m:java.util.stream.Stream.concat(elegibles.stream(),comprometidos.stream()).toList()){
       var d=new LiquidacionDetalle();d.setLiquidacionId(l.getId());d.setMovimientoId(m.getId());d.setConcepto(m.getDescripcion()==null?m.getTipo().name():m.getDescripcion());d.setImporte(m.getMonto());detalles.save(d);
     }
     resultado.add(map(l));
   }
   return new Generacion(resultado.size(),resultado);
 }
 private LocalDateTime corteFinal(LocalDate corte){return corte.plusDays(1).atStartOfDay();}
 public static boolean elegible(MovimientoSaldo m,LocalDateTime fin){return m.getAmbiente()==AmbientePago.PRODUCCION&&m.isConciliado()&&m.getMonto().signum()>0&&m.getEstado()==MovimientoSaldo.EstadoMovimiento.DISPONIBLE&&m.getFechaCreacion()!=null&&m.getFechaCreacion().isBefore(fin)&&m.getFechaDisponible()!=null&&m.getFechaDisponible().isBefore(fin);}
 @Transactional public LiquidacionResponse pagar(Long id,PagoLiquidacionRequest r,byte[] archivo,String tipo,String verificador){
   var l=bloqueada(id);
   if(!r.verificada()||r.monto().compareTo(l.getMonto())!=0)throw new IllegalArgumentException("Verifica la transferencia y su importe exacto antes de registrar el pago");
   if(r.fechaOperacion().isAfter(hoy()))throw new IllegalArgumentException("La fecha de operación no puede ser futura");
   String mime=validarConstancia(archivo,tipo),hash=sha(archivo),banco=r.banco().strip().toUpperCase(Locale.ROOT),codigo=r.codigoOperacion().strip().toUpperCase(Locale.ROOT);
   if(l.getEstado()==LiquidacionDocente.Estado.PAGADA){if(codigo.equals(l.getCodigoOperacion())&&banco.equals(l.getBanco())&&hash.equals(l.getConstanciaSha256()))return map(l);throw new IllegalStateException("La liquidación ya está pagada; su constancia no puede modificarse");}
   if(l.getEstado()!=LiquidacionDocente.Estado.PENDIENTE)throw new IllegalStateException("Solo una liquidación pendiente puede pagarse");
   if(repo.existsByConstanciaSha256AndEstado(hash,LiquidacionDocente.Estado.PAGADA))throw new IllegalArgumentException("Esta constancia ya se utilizó para otra liquidación");
   if(repo.existsByBancoAndFechaOperacionAndCodigoOperacionAndEstado(banco,r.fechaOperacion(),codigo,LiquidacionDocente.Estado.PAGADA))throw new IllegalArgumentException("Esta operación bancaria ya fue registrada");
   var componentes=detalles.findByLiquidacionIdOrderById(id);
   if(componentes.stream().noneMatch(d->d.getImporte().signum()>0))throw new IllegalStateException("La liquidación no tiene operaciones verificables");
   for(var detalle:componentes){
     if(detalle.getImporte().signum()>0 && movimientos.findById(detalle.getMovimientoId()).map(m->m.getAmbiente()!=AmbientePago.PRODUCCION).orElse(true))
       throw new IllegalStateException("La liquidación contiene operaciones sin ambiente de producción verificado");
   }
   BigDecimal saldo=movimientos.findByDocenteId(l.getDocenteId()).stream().filter(m->m.getAmbiente()==AmbientePago.PRODUCCION&&m.isConciliado()&&(m.getEstado()==MovimientoSaldo.EstadoMovimiento.DISPONIBLE||m.getEstado()==MovimientoSaldo.EstadoMovimiento.RESERVADO||m.getEstado()==MovimientoSaldo.EstadoMovimiento.PAGADO)).map(MovimientoSaldo::getMonto).reduce(BigDecimal.ZERO,BigDecimal::add);
   if(saldo.signum()<0)throw new IllegalStateException("El saldo cambió por una devolución o bloqueo. Revisa la liquidación antes de pagar");
   var reserva=movimientos.findById(l.getMovimientoId()).orElseThrow();if(reserva.getEstado()!=MovimientoSaldo.EstadoMovimiento.RESERVADO)throw new IllegalStateException("La reserva ya no está vigente");
   reserva.setEstado(MovimientoSaldo.EstadoMovimiento.PAGADO);movimientos.save(reserva);
   l.setEstado(LiquidacionDocente.Estado.PAGADA);l.setBanco(banco);l.setCodigoOperacion(codigo);l.setFechaOperacion(r.fechaOperacion());l.setDestino(r.destino().strip());l.setTitular(r.titular().strip());l.setFechaPago(clock.instant());l.setVerificadoPor(verificador);var comprobante=new ConstanciaLiquidacion();comprobante.setLiquidacionId(id);comprobante.setContenido(archivo);constancias.save(comprobante);l.setConstanciaTipo(mime);l.setConstanciaSha256(hash);repo.saveAndFlush(l);return map(l);
 }
 @Transactional public LiquidacionResponse anular(Long id,String motivo){var l=bloqueada(id);anularInterna(l,motivo);return map(l);}
 @Transactional public void bloquearPendientes(Long docente,String motivo){bloqueo.bloquear(docente);for(var l:repo.findByDocenteIdAndEstado(docente,LiquidacionDocente.Estado.PENDIENTE))anularInterna(l,motivo);}
 private void anularInterna(LiquidacionDocente l,String motivo){if(l.getEstado()==LiquidacionDocente.Estado.PAGADA)throw new IllegalStateException("Una liquidación pagada no se puede anular");if(l.getEstado()==LiquidacionDocente.Estado.ANULADA)return;if(motivo==null||motivo.isBlank())throw new IllegalArgumentException("Indica el motivo de anulación");l.setEstado(LiquidacionDocente.Estado.ANULADA);l.setMotivoAnulacion(motivo.substring(0,Math.min(motivo.length(),300)));movimientos.findById(l.getMovimientoId()).ifPresent(m->{m.setEstado(MovimientoSaldo.EstadoMovimiento.REVERTIDO);movimientos.save(m);});repo.save(l);}
 private LiquidacionDocente bloqueada(Long id){Long docente=repo.docenteDe(id).orElseThrow(()->new ResourceNotFoundException("Liquidación no encontrada"));bloqueo.bloquear(docente);return repo.bloquear(id).orElseThrow();}
 private boolean verificada(LiquidacionDocente l){var ds=detalles.findByLiquidacionIdOrderById(l.getId());return ds.stream().anyMatch(d->d.getImporte().signum()>0)&&ds.stream().filter(d->d.getImporte().signum()>0).allMatch(d->movimientos.findById(d.getMovimientoId()).map(m->m.getAmbiente()==AmbientePago.PRODUCCION).orElse(false));}
 @Transactional(readOnly=true) public List<LiquidacionResponse> listar(){return repo.findAllByOrderByIdDesc().stream().filter(this::verificada).map(this::map).toList();}
 @Transactional(readOnly=true) public List<LiquidacionResponse> docente(Long id){return repo.findByDocenteIdOrderByIdDesc(id).stream().filter(this::verificada).map(this::map).toList();}
 @Transactional(readOnly=true) public LiquidacionResponse obtener(Long id){return map(buscar(id));}
 @Transactional(readOnly=true) public List<LiquidacionDetalle> detalle(Long id){buscar(id);return detalles.findByLiquidacionIdOrderById(id);}
 public record Constancia(byte[] bytes,String tipo){}
 @Transactional(readOnly=true) public Constancia constancia(Long id){var l=buscar(id);var archivo=constancias.findById(id).orElseThrow(()->new ResourceNotFoundException("La liquidación todavía no tiene constancia"));return new Constancia(archivo.getContenido(),l.getConstanciaTipo());}
 private LiquidacionDocente buscar(Long id){return repo.findById(id).orElseThrow(()->new ResourceNotFoundException("Liquidación no encontrada"));}
 private LiquidacionResponse map(LiquidacionDocente l){return new LiquidacionResponse(l.getId(),l.getDocenteId(),l.getDesde(),l.getCorte(),l.getEstado().name(),l.getMonto(),l.getBaseAcumulada(),l.getCompromisosAnteriores(),l.getFechaCreacion(),l.getFechaPago(),l.getBanco(),l.getCodigoOperacion(),l.getFechaOperacion(),l.getDestino(),l.getTitular(),l.getMotivoAnulacion(),l.getConstanciaSha256()!=null);}
 public static String validarConstancia(byte[] a,String tipo){if(a==null||a.length<5||a.length>5*1024*1024)throw new IllegalArgumentException("Adjunta una constancia PDF, PNG o JPG de hasta 5 MB");if("application/pdf".equals(tipo)&&a[0]=='%'&&a[1]=='P'&&a[2]=='D'&&a[3]=='F'&&a[4]=='-')return tipo;if("image/png".equals(tipo)&&a.length>=8&&Arrays.equals(Arrays.copyOf(a,8),new byte[]{(byte)137,80,78,71,13,10,26,10}))return tipo;if("image/jpeg".equals(tipo)&&a[0]==(byte)255&&a[1]==(byte)216&&a[2]==(byte)255)return tipo;throw new IllegalArgumentException("El contenido no corresponde a un PDF, PNG o JPG válido");}
 private static String sha(byte[] a){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(a));}catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
}
