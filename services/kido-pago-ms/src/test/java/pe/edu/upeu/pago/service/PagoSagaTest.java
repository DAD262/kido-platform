package pe.edu.upeu.pago.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.upeu.pago.entity.OrdenCompra;
import pe.edu.upeu.pago.entity.MovimientoSaldo;
import pe.edu.upeu.pago.client.MercadoPagoClient;
import pe.edu.upeu.pago.repository.OrdenCompraRepository;
import pe.edu.upeu.pago.repository.MovimientoSaldoRepository;

@ExtendWith(MockitoExtension.class)
class PagoSagaTest {
    @Mock private OrdenCompraRepository ordenRepo;
    @Mock private MovimientoSaldoRepository movimientoRepo;
    @Mock private MercadoPagoClient mp;
    @InjectMocks private PagoService servicio;

    private OrdenCompra orden() {
        OrdenCompra orden = new OrdenCompra();
        orden.setId(5L); orden.setEstudianteId(30L); orden.setCursoId(2L);
        orden.setEstado(OrdenCompra.EstadoOrden.APROBADA);
        orden.setMercadoPagoPaymentId("SIM-5");
        return orden;
    }

    @Test void compensacionEsIdempotente() {
        OrdenCompra orden = orden();
        when(ordenRepo.findById(5L)).thenReturn(Optional.of(orden));
        when(movimientoRepo.findByOrdenIdAndTipo(eq(5L), eq(MovimientoSaldo.TipoMovimiento.VENTA)))
            .thenReturn(Optional.empty());
        assertTrue(servicio.compensarCompraPorFalloInscripcion(5L, 30L, 2L, "fallo"));
        assertEquals(OrdenCompra.EstadoOrden.REEMBOLSADA, orden.getEstado());
        assertFalse(servicio.compensarCompraPorFalloInscripcion(5L, 30L, 2L, "fallo repetido"));
        verify(ordenRepo, times(1)).save(orden);
        verifyNoInteractions(mp);
    }

    @Test void falloTardioNoRevierteAccesoYaConfirmado() {
        OrdenCompra orden = orden(); orden.setInscripcionSincronizada(true);
        when(ordenRepo.findById(5L)).thenReturn(Optional.of(orden));
        assertFalse(servicio.compensarCompraPorFalloInscripcion(5L, 30L, 2L, "tardio"));
        assertEquals(OrdenCompra.EstadoOrden.APROBADA, orden.getEstado());
        verifyNoInteractions(mp);
    }

    @Test void compensacionRevierteLaVentaUnaSolaVez() {
        OrdenCompra orden = orden();
        MovimientoSaldo venta = new MovimientoSaldo();
        venta.setEstado(MovimientoSaldo.EstadoMovimiento.PENDIENTE);
        when(ordenRepo.findById(5L)).thenReturn(Optional.of(orden));
        when(movimientoRepo.findByOrdenIdAndTipo(5L, MovimientoSaldo.TipoMovimiento.VENTA))
            .thenReturn(Optional.of(venta));
        assertTrue(servicio.compensarCompraPorFalloInscripcion(5L, 30L, 2L, "rechazo"));
        assertEquals(MovimientoSaldo.EstadoMovimiento.REVERTIDO, venta.getEstado());
        assertFalse(servicio.compensarCompraPorFalloInscripcion(5L, 30L, 2L, "duplicado"));
        verify(movimientoRepo, times(1)).save(venta);
        verify(ordenRepo, times(1)).save(orden);
        verifyNoInteractions(mp);
    }

    @Test void eventoDeOtraCompraNoPuedeCompensarOrden() {
        when(ordenRepo.findById(5L)).thenReturn(Optional.of(orden()));
        assertThrows(IllegalArgumentException.class,
            () -> servicio.compensarCompraPorFalloInscripcion(5L, 999L, 2L, "suplantado"));
        verifyNoInteractions(mp);
    }
}
