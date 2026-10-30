package pe.edu.upeu.pago.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import jakarta.persistence.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.upeu.pago.client.CursoClient;
import pe.edu.upeu.pago.dto.*;
import pe.edu.upeu.pago.entity.*;
import pe.edu.upeu.pago.repository.OrdenCompraRepository;
import java.math.BigDecimal;
import java.util.*;

@ExtendWith(MockitoExtension.class)
class CarritoServiceTest {
 @Mock EntityManager em;@Mock Query query;@Mock CursoClient cursos;@Mock OrdenCompraRepository ordenes;@Mock PagoService pagos;
 @InjectMocks CarritoService service;
 CarritoEstudiante carrito;
 @BeforeEach void setup(){carrito=new CarritoEstudiante();carrito.setEstudianteId(7L);when(em.createNativeQuery(anyString())).thenReturn(query);when(query.setParameter("id",7L)).thenReturn(query);when(em.find(CarritoEstudiante.class,7L,LockModeType.PESSIMISTIC_WRITE)).thenReturn(carrito);}
 CursoCompraDto curso(Long id,String tipo,String estado,String precio){return new CursoCompraDto(id,"Curso "+id,tipo,new BigDecimal(precio),3L,estado,List.of(1L),100,0,true,BigDecimal.ZERO);}
 @Test void agregarEsIdempotenteYTotalUsaPrecioBackend(){when(cursos.obtener(2L)).thenReturn(curso(2L,"PAGO","PUBLICADO","79.90"));service.agregar(7L,2L);var r=service.agregar(7L,2L);assertEquals(1,r.items().size());assertEquals(new BigDecimal("79.90"),r.total());verifyNoInteractions(pagos);}
 @Test void rechazaGratuitoYNoPublicado(){when(cursos.obtener(2L)).thenReturn(curso(2L,"GRATUITO","PUBLICADO","0"));assertThrows(IllegalStateException.class,()->service.agregar(7L,2L));when(cursos.obtener(2L)).thenReturn(curso(2L,"PAGO","BORRADOR","5"));assertThrows(IllegalStateException.class,()->service.agregar(7L,2L));assertTrue(carrito.getCursos().isEmpty());}
 @Test void compraAprobadaSeExcluyeDelTotalYNoSePuedeComprarOtraVez(){carrito.getCursos().add(2L);when(cursos.obtener(2L)).thenReturn(curso(2L,"PAGO","PUBLICADO","50"));when(ordenes.existsByEstudianteIdAndCursoIdAndEstado(7L,2L,OrdenCompra.EstadoOrden.APROBADA)).thenReturn(true);var r=service.obtener(7L);assertFalse(r.items().getFirst().disponible());assertEquals(0,r.total().signum());assertThrows(IllegalStateException.class,()->service.preparar(7L,new CarritoService.Comprar(2L,new BigDecimal("50"))));verifyNoInteractions(pagos);}
 @Test void cambioDePrecioRequiereNuevaRevision(){carrito.getCursos().add(2L);when(cursos.obtener(2L)).thenReturn(curso(2L,"PAGO","PUBLICADO","60"));assertThrows(IllegalStateException.class,()->service.preparar(7L,new CarritoService.Comprar(2L,new BigDecimal("50"))));verifyNoInteractions(pagos);assertTrue(carrito.getCursos().contains(2L));}
 @Test void noPermiteComprarCursoAjenoAlCarrito(){assertThrows(IllegalArgumentException.class,()->service.preparar(7L,new CarritoService.Comprar(2L,BigDecimal.ONE)));verifyNoInteractions(pagos,cursos);}
 @Test void checkoutExigeTodosLosCursosYTotalActual(){carrito.getCursos().addAll(List.of(2L,3L));assertThrows(IllegalStateException.class,()->service.validarTodos(7L,List.of(2L),new BigDecimal("50")));when(cursos.obtener(2L)).thenReturn(curso(2L,"PAGO","PUBLICADO","50"));when(cursos.obtener(3L)).thenReturn(curso(3L,"PAGO","PUBLICADO","20"));assertThrows(IllegalStateException.class,()->service.validarTodos(7L,List.of(2L,3L),new BigDecimal("50")));assertEquals(2,service.validarTodos(7L,List.of(2L,3L),new BigDecimal("70")).size());verifyNoInteractions(pagos);}
 @Test void resumenIdentificaSoloComprasAprobadasDeEstaCuenta(){var pagada=new OrdenCompra();pagada.setCursoId(2L);pagada.setEstado(OrdenCompra.EstadoOrden.APROBADA);var pendiente=new OrdenCompra();pendiente.setCursoId(10L);pendiente.setEstado(OrdenCompra.EstadoOrden.PAGO_INICIADO);when(ordenes.findByEstudianteIdOrderByIdDesc(7L)).thenReturn(List.of(pagada,pendiente));assertEquals(List.of(2L),service.obtener(7L).comprados());verify(ordenes).findByEstudianteIdOrderByIdDesc(7L);}
 @Test void quitarEsIdempotenteYVaciarNoCreaPagos(){carrito.getCursos().add(2L);assertTrue(service.quitar(7L,2L).items().isEmpty());assertTrue(service.quitar(7L,2L).items().isEmpty());assertTrue(service.vaciar(7L).items().isEmpty());verifyNoInteractions(pagos);}
 @Test void cursoRetiradoPuedeEliminarse(){carrito.getCursos().add(2L);when(cursos.obtener(2L)).thenThrow(new pe.edu.upeu.pago.exception.ResourceNotFoundException("Retirado"));var r=service.obtener(7L);assertFalse(r.items().getFirst().disponible());assertTrue(service.quitar(7L,2L).items().isEmpty());}
 @Test void limiteDeVeinteNoAdmiteOtroCurso(){for(long i=10;i<30;i++)carrito.getCursos().add(i);when(cursos.obtener(2L)).thenReturn(curso(2L,"PAGO","PUBLICADO","50"));assertThrows(IllegalStateException.class,()->service.agregar(7L,2L));assertEquals(20,carrito.getCursos().size());}
 @Test void preparaOrdenSinQuitarCursoNiIniciarCobros(){carrito.getCursos().add(2L);when(cursos.obtener(2L)).thenReturn(curso(2L,"PAGO","PUBLICADO","50"));var orden=mock(OrdenResponse.class);when(orden.montoTotal()).thenReturn(new BigDecimal("50"));when(pagos.crear(new CrearOrdenRequest(7L,2L))).thenReturn(orden);assertSame(orden,service.preparar(7L,new CarritoService.Comprar(2L,new BigDecimal("50"))));assertTrue(carrito.getCursos().contains(2L));verify(pagos).crear(new CrearOrdenRequest(7L,2L));verifyNoMoreInteractions(pagos);}
}
