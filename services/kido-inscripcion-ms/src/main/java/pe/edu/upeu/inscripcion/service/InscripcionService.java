package pe.edu.upeu.inscripcion.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.inscripcion.dto.*;
import pe.edu.upeu.inscripcion.entity.*;
import pe.edu.upeu.inscripcion.exception.ResourceNotFoundException;
import pe.edu.upeu.inscripcion.mapper.InscripcionMapper;
import pe.edu.upeu.inscripcion.repository.InscripcionRepository;
import java.time.LocalDateTime;
import java.util.*;

@Service @RequiredArgsConstructor
public class InscripcionService {
 private final InscripcionRepository repository;
 private final InscripcionMapper mapper;

 @Transactional(readOnly=true)
 public List<InscripcionResponse> listar(){return repository.findAllConProgresos().stream().map(mapper::toResponse).toList();}
 @Transactional(readOnly=true)
 public InscripcionResponse obtener(Long id){return mapper.toResponse(buscar(id));}

 @Transactional
 public InscripcionResponse crear(InscripcionRequest r){
   if(repository.existsByEstudianteIdAndCursoId(r.estudianteId(),r.cursoId()))
     throw new IllegalArgumentException("El estudiante ya está inscrito en este curso");
   Inscripcion i=new Inscripcion();
   i.setEstudianteId(r.estudianteId()); i.setCursoId(r.cursoId());
   try {i.setTipoAcceso(Inscripcion.TipoAcceso.valueOf(r.tipoAcceso().toUpperCase()));}
   catch(Exception e){throw new IllegalArgumentException("tipoAcceso debe ser GRATUITO o COMPRA");}
   if(i.getTipoAcceso()==Inscripcion.TipoAcceso.COMPRA)
     throw new IllegalArgumentException("Las inscripciones por COMPRA se crearán únicamente después del pago aprobado en la Unidad 2");
   i.setEstado(Inscripcion.EstadoInscripcion.ACTIVA);
   i.setFechaInscripcion(LocalDateTime.now());
   r.leccionIds().stream().distinct().forEach(id->{ProgresoLeccion p=new ProgresoLeccion();p.setLeccionId(id);p.setInscripcion(i);i.getProgresos().add(p);});
   return mapper.toResponse(repository.save(i));
 }

 @Transactional
 public InscripcionResponse crearPorCompra(CompraInscripcionRequest r){
   Optional<Inscripcion> existente=repository.findByEstudianteIdAndCursoIdConProgresos(r.estudianteId(),r.cursoId());
   if(existente.isPresent()){
     Inscripcion actual=existente.get();
     if(actual.getTipoAcceso()!=Inscripcion.TipoAcceso.COMPRA)
       throw new IllegalStateException("Ya existe una inscripción gratuita para este estudiante y curso");
     if(actual.getEstado()==Inscripcion.EstadoInscripcion.REVOCADA || actual.getEstado()==Inscripcion.EstadoInscripcion.CANCELADA){
       actual.setEstado(Inscripcion.EstadoInscripcion.ACTIVA);
       actual.getProgresos().forEach(p->{p.setCompletada(false);p.setFechaCompletada(null);});
       Set<Long> existentes=actual.getProgresos().stream().map(ProgresoLeccion::getLeccionId).collect(java.util.stream.Collectors.toSet());
       r.leccionIds().stream().filter(Objects::nonNull).filter(id->!existentes.contains(id)).distinct().forEach(leccionId->{
         ProgresoLeccion p=new ProgresoLeccion(); p.setLeccionId(leccionId); p.setInscripcion(actual); actual.getProgresos().add(p);
       });
     }
     return mapper.toResponse(repository.save(actual));
   }
   Inscripcion i=new Inscripcion();
   i.setEstudianteId(r.estudianteId()); i.setCursoId(r.cursoId());
   i.setTipoAcceso(Inscripcion.TipoAcceso.COMPRA);
   i.setEstado(Inscripcion.EstadoInscripcion.ACTIVA);
   i.setFechaInscripcion(LocalDateTime.now());
   r.leccionIds().stream().filter(Objects::nonNull).distinct().forEach(leccionId->{
     ProgresoLeccion p=new ProgresoLeccion(); p.setLeccionId(leccionId); p.setInscripcion(i); i.getProgresos().add(p);
   });
   return mapper.toResponse(repository.save(i));
 }

 @Transactional(readOnly=true)
 public InscripcionResponse buscarPorEstudianteCurso(Long estudianteId, Long cursoId){
   return mapper.toResponse(repository.findByEstudianteIdAndCursoIdConProgresos(estudianteId,cursoId)
     .orElseThrow(()->new ResourceNotFoundException("Inscripción no encontrada para estudiante "+estudianteId+" y curso "+cursoId)));
 }

 @Transactional
 public InscripcionResponse revocarPorReembolso(Long estudianteId, Long cursoId){
   Inscripcion i=repository.findByEstudianteIdAndCursoIdConProgresos(estudianteId,cursoId)
     .orElseThrow(()->new ResourceNotFoundException("Inscripción no encontrada para revocar"));
   if(i.getTipoAcceso()!=Inscripcion.TipoAcceso.COMPRA)
     throw new IllegalArgumentException("Solo una inscripción por compra puede revocarse por reembolso");
   i.setEstado(Inscripcion.EstadoInscripcion.REVOCADA);
   return mapper.toResponse(repository.save(i));
 }

 @Transactional
 public InscripcionResponse cambiarEstado(Long id, EstadoRequest r){
   Inscripcion i=buscar(id);
   try{i.setEstado(Inscripcion.EstadoInscripcion.valueOf(r.estado().toUpperCase()));}
   catch(Exception e){throw new IllegalArgumentException("Estado de inscripción no válido");}
   return mapper.toResponse(repository.save(i));
 }

 @Transactional
 public InscripcionResponse completarLeccion(Long id, Long leccionId){
   Inscripcion i=buscar(id);
   if(i.getEstado()!=Inscripcion.EstadoInscripcion.ACTIVA) throw new IllegalArgumentException("La inscripción no está activa");
   ProgresoLeccion p=i.getProgresos().stream().filter(x->x.getLeccionId().equals(leccionId)).findFirst()
     .orElseThrow(()->new ResourceNotFoundException("La lección no pertenece a esta inscripción"));
   p.setCompletada(true); p.setFechaCompletada(LocalDateTime.now());
   if(i.getProgresos().stream().allMatch(ProgresoLeccion::isCompletada)) i.setEstado(Inscripcion.EstadoInscripcion.COMPLETADA);
   return mapper.toResponse(repository.save(i));
 }

 @Transactional public void eliminar(Long id){repository.delete(buscar(id));}
 private Inscripcion buscar(Long id){return repository.findByIdConProgresos(id).orElseThrow(()->new ResourceNotFoundException("Inscripción no encontrada: "+id));}
}
