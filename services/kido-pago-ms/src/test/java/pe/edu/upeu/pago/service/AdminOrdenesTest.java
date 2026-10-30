package pe.edu.upeu.pago.service;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.upeu.pago.repository.OrdenCompraRepository;
import pe.edu.upeu.pago.entity.*;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class AdminOrdenesTest {
 @Mock OrdenCompraRepository repo;@InjectMocks PagoService service;
 @Test void listaOperativaIncluyeTodosLosAmbientes(){var real=orden(1L,AmbientePago.PRODUCCION);var prueba=orden(2L,AmbientePago.PRUEBAS);var historica=orden(3L,AmbientePago.DESCONOCIDO);when(repo.findAll()).thenReturn(List.of(real,prueba,historica));var resultado=service.listar();assertEquals(3,resultado.size());assertEquals(List.of("DESCONOCIDO","PRUEBAS","PRODUCCION"),resultado.stream().map(x->x.ambiente()).toList());}
 private OrdenCompra orden(Long id,AmbientePago ambiente){var o=new OrdenCompra();o.setId(id);o.setEstado(OrdenCompra.EstadoOrden.PENDIENTE);o.setAmbiente(ambiente);return o;}
}
