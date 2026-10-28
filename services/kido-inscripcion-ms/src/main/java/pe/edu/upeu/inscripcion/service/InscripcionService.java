package pe.edu.upeu.inscripcion.service;
import lombok.RequiredArgsConstructor; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional; import pe.edu.upeu.inscripcion.dto.*; import pe.edu.upeu.inscripcion.entity.*; import pe.edu.upeu.inscripcion.exception.ResourceNotFoundException; import pe.edu.upeu.inscripcion.mapper.InscripcionMapper; import pe.edu.upeu.inscripcion.repository.*; import java.math.BigDecimal; import java.time.*; import java.util.*;
@Service @RequiredArgsConstructor
public class InscripcionService {
 private final pe.edu.upeu.inscripcion.client.CertificadoContextoClient contexto;
 private final InscripcionRepository repository; private final AsistenciaRepository asistenciaRepo; private final CertificadoRepository certificadoRepo; private final InscripcionMapper mapper; private final pe.edu.upeu.inscripcion.messaging.AcademicoEventosProducer eventos;
 @Transactional(readOnly=true) public List<InscripcionResponse> listar(){return repository.findAllConProgresos().stream().map(mapper::toResponse).toList();}
 @Transactional(readOnly=true) public List<InscripcionResponse> porEstudiante(Long id){return repository.findByEstudianteIdOrderByIdDesc(id).stream().map(mapper::toResponse).toList();}
 @Transactional(readOnly=true) public List<InscripcionResponse> porCurso(Long id){return repository.findByCursoIdOrderByIdDesc(id).stream().map(mapper::toResponse).toList();}
 @Transactional(readOnly=true) public InscripcionResponse obtener(Long id){return mapper.toResponse(buscar(id));}
 private void reglas(Inscripcion i,Integer progreso,Integer asistencia,Boolean cert,BigDecimal costo){i.setProgresoMinimo(progreso==null?100:Math.max(0,Math.min(100,progreso)));i.setAsistenciaMinima(asistencia==null?0:Math.max(0,Math.min(100,asistencia)));i.setCertificadoHabilitado(Boolean.TRUE.equals(cert));i.setCertificadoCosto(costo==null?BigDecimal.ZERO:costo.max(BigDecimal.ZERO));}
 private void progresos(Inscripcion i,List<Long> ids){if(ids!=null) ids.stream().filter(Objects::nonNull).distinct().forEach(id->{ProgresoLeccion p=new ProgresoLeccion();p.setLeccionId(id);p.setInscripcion(i);i.getProgresos().add(p);});}
 @Transactional public InscripcionResponse crear(InscripcionRequest r){if(repository.existsByEstudianteIdAndCursoId(r.estudianteId(),r.cursoId()))throw new IllegalArgumentException("El estudiante ya está inscrito en este curso");Inscripcion i=new Inscripcion();i.setEstudianteId(r.estudianteId());i.setCursoId(r.cursoId());try{i.setTipoAcceso(Inscripcion.TipoAcceso.valueOf(r.tipoAcceso().toUpperCase()));}catch(Exception e){throw new IllegalArgumentException("tipoAcceso debe ser GRATUITO o COMPRA");}if(i.getTipoAcceso()==Inscripcion.TipoAcceso.COMPRA)throw new IllegalArgumentException("Las inscripciones por COMPRA solo se crean tras un pago aprobado");i.setEstado(Inscripcion.EstadoInscripcion.ACTIVA);i.setFechaInscripcion(LocalDateTime.now());var curso=contexto.curso(r.cursoId());if(!"GRATUITO".equals(curso.tipo())||!"PUBLICADO".equals(curso.estado()))throw new IllegalArgumentException("Solo puedes inscribirte gratis en un curso gratuito publicado");reglas(i,curso.progresoMinimo(),curso.asistenciaMinima(),curso.certificadoHabilitado(),curso.certificadoCosto());progresos(i,curso.leccionIds());return mapper.toResponse(repository.save(i));}
 @Transactional public InscripcionResponse crearPorCompra(CompraInscripcionRequest r){Optional<Inscripcion> ex=repository.findByEstudianteIdAndCursoIdConProgresos(r.estudianteId(),r.cursoId());if(ex.isPresent()){Inscripcion i=ex.get();if(i.getTipoAcceso()!=Inscripcion.TipoAcceso.COMPRA)throw new IllegalStateException("Ya existe una inscripción gratuita");if(i.getEstado()==Inscripcion.EstadoInscripcion.REVOCADA||i.getEstado()==Inscripcion.EstadoInscripcion.CANCELADA)i.setEstado(Inscripcion.EstadoInscripcion.ACTIVA);reglas(i,r.progresoMinimo(),r.asistenciaMinima(),r.certificadoHabilitado(),r.certificadoCosto());Set<Long> existentes=new HashSet<>(i.getProgresos().stream().map(ProgresoLeccion::getLeccionId).toList());if(r.leccionIds()!=null)r.leccionIds().stream().filter(x->!existentes.contains(x)).forEach(x->{ProgresoLeccion p=new ProgresoLeccion();p.setLeccionId(x);p.setInscripcion(i);i.getProgresos().add(p);});return mapper.toResponse(repository.save(i));}Inscripcion i=new Inscripcion();i.setEstudianteId(r.estudianteId());i.setCursoId(r.cursoId());i.setTipoAcceso(Inscripcion.TipoAcceso.COMPRA);i.setEstado(Inscripcion.EstadoInscripcion.ACTIVA);i.setFechaInscripcion(LocalDateTime.now());reglas(i,r.progresoMinimo(),r.asistenciaMinima(),r.certificadoHabilitado(),r.certificadoCosto());progresos(i,r.leccionIds());return mapper.toResponse(repository.save(i));}

 /** Creates access once per order, with database UNIQUE constraints as the concurrency guard. */
 @Transactional
 public boolean crearPorCompraIdempotente(Long ordenId, CompraInscripcionRequest r) {
   if (ordenId == null || ordenId <= 0 || r == null || r.estudianteId() == null || r.cursoId() == null
       || r.estudianteId() <= 0 || r.cursoId() <= 0) {
     throw new IllegalArgumentException("Evento de compra incompleto");
   }
   Optional<Inscripcion> existente = repository.findByEstudianteIdAndCursoIdConProgresos(r.estudianteId(), r.cursoId());
   if (existente.isPresent()) {
     Inscripcion i = existente.get();
     if (Objects.equals(i.getOrdenCompraId(), ordenId)) {
       // A replay may not reactivate access after a refund/revocation.
       return false;
     }
     if (i.getTipoAcceso() != Inscripcion.TipoAcceso.COMPRA) {
       throw new pe.edu.upeu.inscripcion.exception.InscripcionSagaRechazadaException(
           "Ya existe una inscripción gratuita de este estudiante en el curso");
     }
     if (i.getEstado() != Inscripcion.EstadoInscripcion.REVOCADA
         && i.getEstado() != Inscripcion.EstadoInscripcion.CANCELADA) {
       throw new pe.edu.upeu.inscripcion.exception.InscripcionSagaRechazadaException(
           "El estudiante ya tiene acceso por otra compra");
     }
     i.setOrdenCompraId(ordenId);
     i.setEstado(Inscripcion.EstadoInscripcion.ACTIVA);
     reglas(i, r.progresoMinimo(), r.asistenciaMinima(), r.certificadoHabilitado(), r.certificadoCosto());
     Set<Long> lecciones = new HashSet<>(i.getProgresos().stream().map(ProgresoLeccion::getLeccionId).toList());
     if (r.leccionIds() != null) {
       r.leccionIds().stream().filter(Objects::nonNull).distinct()
           .filter(leccion -> !lecciones.contains(leccion)).forEach(leccion -> {
             ProgresoLeccion p = new ProgresoLeccion();
             p.setLeccionId(leccion); p.setInscripcion(i); i.getProgresos().add(p);
           });
     }
     repository.saveAndFlush(i);
     return true;
   }
   Inscripcion i = new Inscripcion();
   i.setEstudianteId(r.estudianteId()); i.setCursoId(r.cursoId());
   i.setOrdenCompraId(ordenId); i.setTipoAcceso(Inscripcion.TipoAcceso.COMPRA);
   i.setEstado(Inscripcion.EstadoInscripcion.ACTIVA); i.setFechaInscripcion(LocalDateTime.now());
   reglas(i, r.progresoMinimo(), r.asistenciaMinima(), r.certificadoHabilitado(), r.certificadoCosto());
   progresos(i, r.leccionIds());
   repository.saveAndFlush(i);
   return true;
 }

 @Transactional(readOnly=true)
 public boolean existeCompraDeOrden(Long ordenId) {
   return ordenId != null && repository.existsByOrdenCompraId(ordenId);
 }

 @Transactional(readOnly=true)
 public boolean existeCompraActivaDeOtraOrden(Long estudianteId, Long cursoId, Long ordenId) {
   return repository.findByEstudianteIdAndCursoIdConProgresos(estudianteId, cursoId)
       .filter(i -> !Objects.equals(i.getOrdenCompraId(), ordenId))
       .filter(i -> i.getEstado() == Inscripcion.EstadoInscripcion.ACTIVA
           || i.getEstado() == Inscripcion.EstadoInscripcion.COMPLETADA).isPresent();
 }
 @Transactional(readOnly=true) public InscripcionResponse buscarPorEstudianteCurso(Long e,Long c){return mapper.toResponse(repository.findByEstudianteIdAndCursoIdConProgresos(e,c).orElseThrow(()->new ResourceNotFoundException("Inscripción no encontrada")));}
 @Transactional public InscripcionResponse revocarPorReembolso(Long e,Long c){Inscripcion i=repository.findByEstudianteIdAndCursoIdConProgresos(e,c).orElseThrow(()->new ResourceNotFoundException("Inscripción no encontrada"));if(i.getTipoAcceso()!=Inscripcion.TipoAcceso.COMPRA)throw new IllegalArgumentException("Solo una inscripción por compra puede revocarse");i.setEstado(Inscripcion.EstadoInscripcion.REVOCADA);certificadoRepo.findByInscripcionId(i.getId()).ifPresent(x->{x.setEstado(Certificado.EstadoCertificado.ANULADO);certificadoRepo.save(x);});return mapper.toResponse(repository.save(i));}
 @Transactional public InscripcionResponse cambiarEstado(Long id,EstadoRequest r){Inscripcion i=buscar(id);try{i.setEstado(Inscripcion.EstadoInscripcion.valueOf(r.estado().toUpperCase()));}catch(Exception e){throw new IllegalArgumentException("Estado no válido");}return mapper.toResponse(repository.save(i));}
 @Transactional public InscripcionResponse completarLeccion(Long id,Long leccionId){Inscripcion i=bloquear(id);if(i.getEstado()!=Inscripcion.EstadoInscripcion.ACTIVA)throw new IllegalArgumentException("La inscripción no está activa");ProgresoLeccion p=i.getProgresos().stream().filter(x->x.getLeccionId().equals(leccionId)).findFirst().orElseThrow(()->new ResourceNotFoundException("La lección no pertenece a esta inscripción"));p.setCompletada(true);p.setFechaCompletada(LocalDateTime.now());boolean completo=evaluarYCerrar(i);Inscripcion guardada=repository.save(i);if(completo) publicar(i,"curso.completado");return mapper.toResponse(guardada);}
 @Transactional public AsistenciaResponse registrarAsistencia(Long id,AsistenciaRequest r){Inscripcion i=bloquear(id);Asistencia a=asistenciaRepo.findByInscripcionIdAndFechaClase(id,r.fechaClase()).orElseGet(Asistencia::new);a.setInscripcion(i);a.setFechaClase(r.fechaClase());try{a.setEstado(Asistencia.EstadoAsistencia.valueOf(r.estado().toUpperCase()));}catch(Exception e){throw new IllegalArgumentException("Estado de asistencia no válido");}a.setObservacion(r.observacion());a.setFechaRegistro(LocalDateTime.now());a=asistenciaRepo.save(a);boolean completo=evaluarYCerrar(i);repository.save(i);if(completo) publicar(i,"curso.completado");return map(a);}
 @Transactional(readOnly=true) public List<AsistenciaResponse> asistencias(Long id){buscar(id);return asistenciaRepo.findByInscripcionIdOrderByFechaClaseDesc(id).stream().map(this::map).toList();}
 @Transactional(readOnly=true) public EstadoAcademicoResponse estadoAcademico(Long id){Inscripcion i=buscar(id);return estado(i);}
 private int progreso(Inscripcion i){if(i.getProgresos().isEmpty())return 0;return (int)Math.round(i.getProgresos().stream().filter(ProgresoLeccion::isCompletada).count()*100.0/i.getProgresos().size());}
 private int asistencia(Long id){long total=asistenciaRepo.countByInscripcionId(id);if(total==0)return 0;long presentes=asistenciaRepo.countByInscripcionIdAndEstadoIn(id,List.of(Asistencia.EstadoAsistencia.PRESENTE,Asistencia.EstadoAsistencia.TARDE,Asistencia.EstadoAsistencia.JUSTIFICADA));return (int)Math.round(presentes*100.0/total);}
 private EstadoAcademicoResponse estado(Inscripcion i){int p=progreso(i),a=asistencia(i.getId());boolean cp=p>=i.getProgresoMinimo(),ca=a>=i.getAsistenciaMinima();return new EstadoAcademicoResponse(i.getId(),p,a,i.getProgresoMinimo(),i.getAsistenciaMinima(),cp,ca,i.isCertificadoHabilitado()&&cp&&ca,i.getEstado().name());}
 private boolean evaluarYCerrar(Inscripcion i){EstadoAcademicoResponse e=estado(i);boolean cumple=e.cumpleProgreso()&&e.cumpleAsistencia();boolean nuevo=cumple&&i.getEstado()==Inscripcion.EstadoInscripcion.ACTIVA;if(nuevo)i.setEstado(Inscripcion.EstadoInscripcion.COMPLETADA);else if(!cumple&&i.getEstado()==Inscripcion.EstadoInscripcion.COMPLETADA)i.setEstado(Inscripcion.EstadoInscripcion.ACTIVA);if(cumple&&i.isCertificadoHabilitado()&&(i.getEstado()==Inscripcion.EstadoInscripcion.ACTIVA||i.getEstado()==Inscripcion.EstadoInscripcion.COMPLETADA))emitir(i);return nuevo;}
 private void publicar(Inscripcion i,String tipo){eventos.publicarTrasCommit(pe.edu.upeu.inscripcion.event.AcademicoEvento.builder().tipoEvento(tipo).estudianteId(i.getEstudianteId()).cursoId(i.getCursoId()).estado(i.getEstado().name()).origen("kido-inscripcion-ms").timestamp(java.time.Instant.now().toEpochMilli()).build());}
 @Transactional public CertificadoResponse solicitarCertificado(Long id){Inscripcion i=bloquear(id);incluirBeneficio(i);return map(emitir(i));}
 private Certificado emitir(Inscripcion i){if(i.getEstado()==Inscripcion.EstadoInscripcion.REVOCADA||i.getEstado()==Inscripcion.EstadoInscripcion.CANCELADA)throw new IllegalStateException("La inscripción ya no permite emitir un certificado");if(!i.isCertificadoHabilitado())throw new IllegalStateException("El curso no habilita certificado");if(!estado(i).elegibleCertificado())throw new IllegalStateException("Aún no cumple los requisitos académicos");var actual=certificadoRepo.findByInscripcionId(i.getId());if(actual.isPresent()){if(actual.get().getEstado()==Certificado.EstadoCertificado.ANULADO)throw new IllegalStateException("El certificado está anulado");return actual.get();}var c=new Certificado();c.setInscripcionId(i.getId());c.setCodigo("KIDO-CERT-"+i.getId()+"-"+UUID.randomUUID().toString().substring(0,12).toUpperCase());c.setCosto(i.getCertificadoCosto());boolean incluido=i.getCertificadoCosto().signum()==0;c.setPagoConfirmado(incluido);c.setEstado(incluido?Certificado.EstadoCertificado.DISPONIBLE:Certificado.EstadoCertificado.PENDIENTE_PAGO);if(incluido)c.setFechaEmision(LocalDateTime.now(ZoneId.of("America/Lima")).truncatedTo(java.time.temporal.ChronoUnit.MICROS));var guardado=certificadoRepo.save(c);if(incluido)publicar(i,"certificado.disponible");return guardado;}
 @Transactional public CertificadoResponse confirmarPagoCertificado(Long id){Inscripcion i=bloquear(id);if(i.getEstado()==Inscripcion.EstadoInscripcion.REVOCADA||i.getEstado()==Inscripcion.EstadoInscripcion.CANCELADA||!estado(i).elegibleCertificado())throw new IllegalStateException("La inscripción no permite habilitar el certificado");var c=certificadoRepo.findByInscripcionId(id).orElseThrow(()->new ResourceNotFoundException("Certificado no solicitado"));if(c.getEstado()==Certificado.EstadoCertificado.ANULADO)throw new IllegalStateException("El certificado está anulado");if(c.isPagoConfirmado()&&c.getEstado()==Certificado.EstadoCertificado.DISPONIBLE)return map(c);c.setPagoConfirmado(true);c.setEstado(Certificado.EstadoCertificado.DISPONIBLE);if(c.getFechaEmision()==null)c.setFechaEmision(LocalDateTime.now(ZoneId.of("America/Lima")).truncatedTo(java.time.temporal.ChronoUnit.MICROS));var guardado=certificadoRepo.save(c);publicar(i,"certificado.disponible");return map(guardado);}
 @Transactional public CertificadoResponse certificado(Long id){Inscripcion i=bloquear(id);incluirBeneficio(i);var c=certificadoRepo.findByInscripcionId(id);if(c.isPresent())return map(c.get());if(i.isCertificadoHabilitado()&&estado(i).elegibleCertificado()&&(i.getEstado()==Inscripcion.EstadoInscripcion.ACTIVA||i.getEstado()==Inscripcion.EstadoInscripcion.COMPLETADA))return map(emitir(i));throw new ResourceNotFoundException("Certificado no encontrado");}
 private void incluirBeneficio(Inscripcion i){if(i.isCertificadoHabilitado()||i.getCertificadoCosto().signum()!=0||i.getEstado()==Inscripcion.EstadoInscripcion.REVOCADA||i.getEstado()==Inscripcion.EstadoInscripcion.CANCELADA)return;var c=contexto.curso(i.getCursoId());if(c.certificadoHabilitado()&&c.certificadoCosto()!=null&&c.certificadoCosto().signum()==0&&"PUBLICADO".equals(c.estado())){i.setCertificadoHabilitado(true);repository.save(i);}}
 private Inscripcion bloquear(Long id){return repository.bloquear(id).orElseThrow(()->new ResourceNotFoundException("Inscripción no encontrada"));}
 @Transactional public void eliminar(Long id){repository.delete(buscar(id));}
 private Inscripcion buscar(Long id){return repository.findByIdConProgresos(id).orElseThrow(()->new ResourceNotFoundException("Inscripción no encontrada: "+id));}
 private AsistenciaResponse map(Asistencia a){return new AsistenciaResponse(a.getId(),a.getInscripcion().getId(),a.getFechaClase(),a.getEstado().name(),a.getObservacion(),a.getFechaRegistro());}
 private CertificadoResponse map(Certificado c){return new CertificadoResponse(c.getId(),c.getInscripcionId(),c.getCodigo(),c.getEstado().name(),c.getCosto(),c.isPagoConfirmado(),c.getFechaEmision());}
}
