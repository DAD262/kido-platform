package pe.edu.upeu.pago.service;
import static org.mockito.Mockito.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import pe.edu.upeu.pago.entity.OrdenCompra;
import pe.edu.upeu.pago.repository.OrdenCompraRepository;
class RecuperacionAccesosProgramadaTest {
 @Test void soloReintentaOrdenesAprobadasSinAcceso(){var repo=mock(OrdenCompraRepository.class);var pagos=mock(PagoService.class);var pendiente=orden(35L,OrdenCompra.EstadoOrden.APROBADA,false);var lista=orden(36L,OrdenCompra.EstadoOrden.APROBADA,true);var sinPago=orden(37L,OrdenCompra.EstadoOrden.PAGO_INICIADO,false);when(repo.findTop50ByEstadoAndInscripcionSincronizadaFalseOrderByIdAsc(OrdenCompra.EstadoOrden.APROBADA)).thenReturn(List.of(pendiente,lista,sinPago));new RecuperacionAccesosProgramada(repo,pagos).recuperar();verify(pagos).reintentarInscripcion(35L);verifyNoMoreInteractions(pagos);}
 @Test void unFalloNoBloqueaLosAccesosDeOtrosAlumnos(){var repo=mock(OrdenCompraRepository.class);var pagos=mock(PagoService.class);when(repo.findTop50ByEstadoAndInscripcionSincronizadaFalseOrderByIdAsc(OrdenCompra.EstadoOrden.APROBADA)).thenReturn(List.of(orden(35L,OrdenCompra.EstadoOrden.APROBADA,false),orden(36L,OrdenCompra.EstadoOrden.APROBADA,false)));doThrow(new IllegalStateException("Kafka temporalmente no disponible")).when(pagos).reintentarInscripcion(35L);new RecuperacionAccesosProgramada(repo,pagos).recuperar();verify(pagos).reintentarInscripcion(36L);}
 private OrdenCompra orden(Long id,OrdenCompra.EstadoOrden estado,boolean sincronizada){var o=new OrdenCompra();o.setId(id);o.setEstado(estado);o.setInscripcionSincronizada(sincronizada);return o;}
}
