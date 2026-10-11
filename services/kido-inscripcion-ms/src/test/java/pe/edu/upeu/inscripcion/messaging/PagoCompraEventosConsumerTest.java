package pe.edu.upeu.inscripcion.messaging;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.util.ReflectionTestUtils;
import pe.edu.upeu.inscripcion.dto.CompraInscripcionRequest;
import pe.edu.upeu.inscripcion.event.AcademicoEvento;
import pe.edu.upeu.inscripcion.event.PagoCompraEvento;
import pe.edu.upeu.inscripcion.exception.InscripcionSagaRechazadaException;
import pe.edu.upeu.inscripcion.service.InscripcionService;

@ExtendWith(MockitoExtension.class)
class PagoCompraEventosConsumerTest {
    @Mock private InscripcionService service;
    @Mock private AcademicoEventosProducer producer;
    @InjectMocks private PagoCompraEventosConsumer consumer;

    private PagoCompraEvento evento() {
        return PagoCompraEvento.builder().tipoEvento("pago.aprobado").schemaVersion(2).ordenId(15L)
            .estudianteId(48L).cursoId(2L).estado("APROBADA").origen("kido-pago-ms").build();
    }

    @Test void eventoAntiguoDeS08NoCompensaComprasHistoricas() {
        PagoCompraEvento antiguo = evento();
        antiguo.setSchemaVersion(null);
        consumer.alRecibirPago(antiguo);
        verifyNoInteractions(service, producer);
    }

    @Test void duplicadoIgnoradoPeroAckPublicado() {
        when(service.crearPorCompraIdempotente(eq(15L), any(CompraInscripcionRequest.class))).thenReturn(false);
        consumer.alRecibirPago(evento());
        verify(producer).publicarConfirmado(argThat((AcademicoEvento e) ->
            "inscripcion.activada".equals(e.getTipoEvento()) && e.getOrdenId().equals(15L)));
    }

    @Test void rechazoDeNegocioPideCompensacion() {
        when(service.crearPorCompraIdempotente(eq(15L), any(CompraInscripcionRequest.class)))
            .thenThrow(new InscripcionSagaRechazadaException("conflicto"));
        consumer.alRecibirPago(evento());
        verify(producer).publicarConfirmado(argThat((AcademicoEvento e) ->
            "inscripcion.fallida".equals(e.getTipoEvento())));
    }

    @Test void errorTemporalNoReembolsaYReintenta() {
        when(service.crearPorCompraIdempotente(eq(15L), any(CompraInscripcionRequest.class)))
            .thenThrow(new DataAccessResourceFailureException("postgres no disponible"));
        assertThrows(DataAccessResourceFailureException.class, () -> consumer.alRecibirPago(evento()));
        verifyNoInteractions(producer);
    }

    @Test void uniqueDelMismoPedidoSeReconoceComoDuplicado() {
        when(service.crearPorCompraIdempotente(eq(15L), any(CompraInscripcionRequest.class)))
            .thenThrow(new DataIntegrityViolationException("unique"));
        when(service.existeCompraDeOrden(15L)).thenReturn(true);
        consumer.alRecibirPago(evento());
        verify(producer).publicarConfirmado(argThat((AcademicoEvento e) ->
            "inscripcion.activada".equals(e.getTipoEvento())));
    }

    @Test void falloDeterministaSoloConConfiguracionExplicita() {
        ReflectionTestUtils.setField(consumer, "estudianteFalloDemo", 48L);
        consumer.alRecibirPago(evento());
        verifyNoInteractions(service);
        verify(producer).publicarConfirmado(argThat((AcademicoEvento e) ->
            "inscripcion.fallida".equals(e.getTipoEvento())));
    }
}
