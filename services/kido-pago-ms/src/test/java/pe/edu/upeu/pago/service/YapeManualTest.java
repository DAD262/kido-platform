package pe.edu.upeu.pago.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.upeu.pago.client.CursoClient;
import pe.edu.upeu.pago.client.MercadoPagoClient;
import pe.edu.upeu.pago.entity.AmbientePago;
import pe.edu.upeu.pago.entity.OrdenCompra;
import pe.edu.upeu.pago.messaging.PagoEventosProducer;
import pe.edu.upeu.pago.repository.MovimientoSaldoRepository;
import pe.edu.upeu.pago.repository.OrdenCompraRepository;

@ExtendWith(MockitoExtension.class)
class YapeManualTest {
  @Mock OrdenCompraRepository ordenes;
  @Mock MovimientoSaldoRepository movimientos;
  @Mock CursoClient cursos;
  @Mock PagoEventosProducer eventos;
  @Mock MercadoPagoClient mercadoPago;
  @Mock CertificadoPagoService certificados;
  @Mock LiquidacionService liquidaciones;
  @Mock ConciliacionPagoService conciliacion;
  @Mock PagoCarritoValidador validadorCarrito;
  @InjectMocks PagoService servicio;

  private OrdenCompra orden;

  @BeforeEach
  void preparar() {
    orden = new OrdenCompra();
    orden.setId(91L);
    orden.setAmbiente(AmbientePago.YAPE_MANUAL);
    orden.setEstado(OrdenCompra.EstadoOrden.PENDIENTE);
    orden.setEstudianteId(1001L);
    orden.setCursoId(18L);
    orden.setDocenteId(1000L);
    orden.setCursoTitulo("Curso de demostración");
    orden.setMontoTotal(new BigDecimal("0.99"));
    orden.setComisionKido(new BigDecimal("0.20"));
    orden.setMontoDocente(new BigDecimal("0.79"));
    lenient().when(ordenes.bloquear(91L)).thenReturn(Optional.of(orden));
  }

  @Test
  void importeDistintoNoHabilitaElCurso() {
    assertThrows(IllegalArgumentException.class,
        () -> servicio.confirmarYape(91L, "12345678", new BigDecimal("1.00"), "admin"));
    assertEquals(OrdenCompra.EstadoOrden.PENDIENTE, orden.getEstado());
    verify(ordenes, never()).saveAndFlush(any());
    verifyNoInteractions(eventos);
  }

  @Test
  void operacionInvalidaNoHabilitaElCurso() {
    assertThrows(IllegalArgumentException.class,
        () -> servicio.confirmarYape(91L, "12", new BigDecimal("0.99"), "admin"));
    assertEquals(OrdenCompra.EstadoOrden.PENDIENTE, orden.getEstado());
    verifyNoInteractions(eventos);
  }

  @Test
  void confirmacionValidaApruebaUnaSolaVez() {
    assertEquals("APROBADA",
        servicio.confirmarYape(91L, "yp123456", new BigDecimal("0.99"), "admin-kido").estado());
    assertEquals("YP123456", orden.getYapeOperacion());
    assertEquals("admin-kido", orden.getYapeConfirmadoPor());
    assertEquals("APROBADA",
        servicio.confirmarYape(91L, "YP123456", new BigDecimal("0.99"), "admin-kido").estado());
    verify(ordenes, times(1)).saveAndFlush(orden);
    verify(eventos, times(1)).publicarTrasCommit(any());
    verifyNoInteractions(movimientos);
  }
}
