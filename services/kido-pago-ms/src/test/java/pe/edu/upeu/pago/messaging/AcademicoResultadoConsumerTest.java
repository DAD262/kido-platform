package pe.edu.upeu.pago.messaging;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.upeu.pago.event.AcademicoResultadoEvento;
import pe.edu.upeu.pago.service.PagoService;

@ExtendWith(MockitoExtension.class)
class AcademicoResultadoConsumerTest {
    @Mock private PagoService service;
    @InjectMocks private AcademicoResultadoConsumer consumer;

    private AcademicoResultadoEvento resultado(String tipo) {
        return AcademicoResultadoEvento.builder().tipoEvento(tipo).ordenId(5L)
            .estudianteId(30L).cursoId(2L).origen("kido-inscripcion-ms")
            .motivo("rechazo de negocio").build();
    }

    @Test void eventosAcademicosSinOrdenNoInterrumpenLaSaga() {
        for (String tipo : new String[]{"curso.completado", "certificado.disponible", "otro.evento"}) {
            consumer.alRecibirResultado(AcademicoResultadoEvento.builder()
                .tipoEvento(tipo).estudianteId(30L).cursoId(2L)
                .origen("kido-inscripcion-ms").build());
        }
        verifyNoInteractions(service);
    }

    @Test void resultadoDeSagaSinOrdenSigueSiendoInvalido() {
        AcademicoResultadoEvento evento = resultado("inscripcion.fallida");
        evento.setOrdenId(null);
        assertThrows(IllegalArgumentException.class, () -> consumer.alRecibirResultado(evento));
        verifyNoInteractions(service);
    }

    @Test void confirmacionSeEnrutaALaOrdenCorrespondiente() {
        consumer.alRecibirResultado(resultado("inscripcion.activada"));
        verify(service).marcarInscripcionSincronizada(5L, 30L, 2L);
        verifyNoMoreInteractions(service);
    }

    @Test void falloSeEnrutaALaCompensacionCorrespondiente() {
        consumer.alRecibirResultado(resultado("inscripcion.fallida"));
        verify(service).compensarCompraPorFalloInscripcion(5L, 30L, 2L, "rechazo de negocio");
        verifyNoMoreInteractions(service);
    }
}
