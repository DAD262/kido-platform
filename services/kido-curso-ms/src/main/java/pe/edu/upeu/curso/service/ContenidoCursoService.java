package pe.edu.upeu.curso.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.curso.dto.*;
import pe.edu.upeu.curso.entity.*;
import pe.edu.upeu.curso.exception.ResourceNotFoundException;
import pe.edu.upeu.curso.repository.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service @RequiredArgsConstructor
public class ContenidoCursoService {
 private final CursoRepository cursoRepo; private final ModuloRepository moduloRepo; private final LeccionRepository leccionRepo; private final MaterialRepository materialRepo; private final ConfiguracionCursoRepository configRepo;
 private Curso curso(Long id){return cursoRepo.findById(id).orElseThrow(()->new ResourceNotFoundException("Curso no encontrado: "+id));}
 private Modulo modulo(Long id){return moduloRepo.findById(id).orElseThrow(()->new ResourceNotFoundException("Módulo no encontrado: "+id));}
 private Leccion leccion(Long id){return leccionRepo.findById(id).orElseThrow(()->new ResourceNotFoundException("Lección no encontrada: "+id));}
 private ConfiguracionCurso config(Long cursoId){ return configRepo.findByCursoId(cursoId).orElseGet(()->{ConfiguracionCurso c=new ConfiguracionCurso(); c.setCursoId(cursoId); c.setProgresoMinimo(100); c.setAsistenciaMinima(0); c.setCertificadoHabilitado(false); c.setCertificadoCosto(BigDecimal.ZERO); return c;}); }
 @Transactional public ModuloDetalleResponse crearModulo(Long cursoId,ModuloRequest r){curso(cursoId); Modulo m=new Modulo();m.setCursoId(cursoId);m.setTitulo(r.titulo());m.setOrden(r.orden());return mapModulo(moduloRepo.save(m));}
 @Transactional public ModuloDetalleResponse actualizarModulo(Long id,ModuloRequest r){Modulo m=modulo(id);m.setTitulo(r.titulo());m.setOrden(r.orden());return mapModulo(moduloRepo.save(m));}
 @Transactional public void eliminarModulo(Long id){moduloRepo.delete(modulo(id));}
 @Transactional public LeccionDetalleResponse crearLeccion(Long moduloId,LeccionRequest r){modulo(moduloId);Leccion l=new Leccion();l.setModuloId(moduloId);l.setTitulo(r.titulo());l.setOrden(r.orden());l.setUrlContenido(r.urlContenido());return mapLeccion(leccionRepo.save(l));}
 @Transactional public LeccionDetalleResponse actualizarLeccion(Long id,LeccionRequest r){Leccion l=leccion(id);l.setTitulo(r.titulo());l.setOrden(r.orden());l.setUrlContenido(r.urlContenido());return mapLeccion(leccionRepo.save(l));}
 @Transactional public void eliminarLeccion(Long id){leccionRepo.delete(leccion(id));}
 @Transactional public MaterialResponse crearMaterial(Long leccionId,MaterialRequest r){leccion(leccionId);Material m=new Material();m.setLeccionId(leccionId);m.setNombre(r.nombre());try{m.setTipo(Material.TipoMaterial.valueOf(r.tipo().toUpperCase()));}catch(Exception e){throw new IllegalArgumentException("Tipo de material no válido");}m.setUrl(r.url());m.setDescripcion(r.descripcion());m.setDescargable(r.descargable());m.setFechaCreacion(LocalDateTime.now());return mapMaterial(materialRepo.save(m));}
 @Transactional public MaterialResponse actualizarMaterial(Long id,MaterialRequest r){Material m=materialRepo.findById(id).orElseThrow(()->new ResourceNotFoundException("Material no encontrado: "+id));m.setNombre(r.nombre());try{m.setTipo(Material.TipoMaterial.valueOf(r.tipo().toUpperCase()));}catch(Exception e){throw new IllegalArgumentException("Tipo de material no válido");}m.setUrl(r.url());m.setDescripcion(r.descripcion());m.setDescargable(r.descargable());return mapMaterial(materialRepo.save(m));}
 @Transactional public void eliminarMaterial(Long id){materialRepo.deleteById(id);}
 @Transactional public ConfiguracionCursoResponse configurar(Long cursoId,ConfiguracionCursoRequest r){curso(cursoId); if(!r.certificadoHabilitado() && r.certificadoCosto().compareTo(BigDecimal.ZERO)>0) throw new IllegalArgumentException("Un certificado deshabilitado no puede tener costo"); ConfiguracionCurso c=config(cursoId);c.setProgresoMinimo(r.progresoMinimo());c.setAsistenciaMinima(r.asistenciaMinima());c.setCertificadoHabilitado(r.certificadoHabilitado());c.setCertificadoCosto(r.certificadoCosto());return mapConfig(configRepo.save(c));}
 @Transactional(readOnly=true) public ConfiguracionCursoResponse obtenerConfig(Long cursoId){curso(cursoId);return mapConfig(config(cursoId));}
 @Transactional(readOnly=true) public ContenidoCursoResponse contenido(Long cursoId){Curso c=curso(cursoId);List<ModuloDetalleResponse> ms=moduloRepo.findByCursoIdOrderByOrdenAscIdAsc(cursoId).stream().map(this::mapModulo).toList();return new ContenidoCursoResponse(cursoId,c.getTitulo(),ms,mapConfig(config(cursoId)));}
 private ModuloDetalleResponse mapModulo(Modulo m){return new ModuloDetalleResponse(m.getId(),m.getCursoId(),m.getTitulo(),m.getOrden(),leccionRepo.findByModuloIdOrderByOrdenAscIdAsc(m.getId()).stream().map(this::mapLeccion).toList());}
 private LeccionDetalleResponse mapLeccion(Leccion l){return new LeccionDetalleResponse(l.getId(),l.getModuloId(),l.getTitulo(),l.getOrden(),l.getUrlContenido(),materialRepo.findByLeccionIdOrderByIdAsc(l.getId()).stream().map(this::mapMaterial).toList());}
 private MaterialResponse mapMaterial(Material m){return new MaterialResponse(m.getId(),m.getLeccionId(),m.getNombre(),m.getTipo().name(),m.getUrl(),m.getDescripcion(),m.isDescargable(),m.getFechaCreacion());}
 private ConfiguracionCursoResponse mapConfig(ConfiguracionCurso c){return new ConfiguracionCursoResponse(c.getCursoId(),c.getProgresoMinimo(),c.getAsistenciaMinima(),c.isCertificadoHabilitado(),c.getCertificadoCosto());}
}
