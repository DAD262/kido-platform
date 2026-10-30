package pe.edu.upeu.curso.service;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.upeu.curso.entity.Curso;
import pe.edu.upeu.curso.repository.*;
import pe.edu.upeu.curso.mapper.CursoMapper;
import java.util.*;

@ExtendWith(MockitoExtension.class)
class RetiradaCatalogoTest {
 @Mock CursoRepository cursos;@Mock CategoriaRepository categorias;@Mock LeccionRepository lecciones;@Mock CursoMapper mapper;@Mock ConfiguracionCursoRepository configuracion;
 @InjectMocks CursoService service;
 @Test void eliminarArchivadoConservaCursoYLeccionesHistoricas(){var c=new Curso();c.setId(5L);c.setEstado(Curso.EstadoCurso.ARCHIVADO);when(cursos.findByIdConCategoria(5L)).thenReturn(Optional.of(c));service.eliminar(5L);assertTrue(c.isRetiradoCatalogo());verify(cursos).save(c);verify(cursos,never()).delete(any());verifyNoInteractions(lecciones);}
 @Test void publicadoNoSePuedeRetirarSinArchivar(){var c=new Curso();c.setEstado(Curso.EstadoCurso.PUBLICADO);when(cursos.findByIdConCategoria(1L)).thenReturn(Optional.of(c));assertThrows(IllegalArgumentException.class,()->service.eliminar(1L));verify(cursos,never()).save(any());}
 @Test void retiradoNoApareceEnListadosPeroSigueConsultable(){var c=new Curso();c.setRetiradoCatalogo(true);when(cursos.findAllConCategoria()).thenReturn(List.of(c));when(cursos.findByDocenteIdOrderByIdDesc(1L)).thenReturn(List.of(c));when(cursos.findByEstadoOrderByIdDesc(Curso.EstadoCurso.PUBLICADO)).thenReturn(List.of(c));assertTrue(service.listar().isEmpty());assertTrue(service.porDocente(1L).isEmpty());assertTrue(service.publicados().isEmpty());when(cursos.findByIdConCategoria(5L)).thenReturn(Optional.of(c));service.obtener(5L);verify(mapper).toResponse(c);}
}
