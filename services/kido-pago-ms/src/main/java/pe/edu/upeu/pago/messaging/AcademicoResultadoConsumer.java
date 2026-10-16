package pe.edu.upeu.pago.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import pe.edu.upeu.pago.event.AcademicoResultadoEvento;
import pe.edu.upeu.pago.service.PagoService;

@Slf4j
@Component
@RequiredArgsConstructor
public class AcademicoResultadoConsumer {
    private final PagoService pagoService;

    @KafkaListener(topics = "${app.kafka.topic.academico:kido-academico-eventos}", groupId = "kido-pago-saga-ms")
    public void alRecibirResultado(AcademicoResultadoEvento evento) {
        // This topic also carries course/certificate events without an order.
        // Validate only messages belonging to this Saga, as in the payment consumer.
        if (!"inscripcion.activada".equals(evento.getTipoEvento())
                && !"inscripcion.fallida".equals(evento.getTipoEvento())) {
            return;
        }
        if (!"kido-inscripcion-ms".equals(evento.getOrigen()) || evento.getOrdenId() == null
                || evento.getEstudianteId() == null || evento.getCursoId() == null) {
            throw new IllegalArgumentException("Resultado de Saga incompleto o de origen inesperado");
        }
        if ("inscripcion.activada".equals(evento.getTipoEvento())) {
            boolean aplicado = pagoService.marcarInscripcionSincronizada(evento.getOrdenId(), evento.getEstudianteId(), evento.getCursoId());
            log.info("component=saga-consumer eventType={} ordenId={} status={}",
                evento.getTipoEvento(), evento.getOrdenId(), aplicado ? "processed" : "ignored");
        } else if ("inscripcion.fallida".equals(evento.getTipoEvento())) {
            boolean aplicada = pagoService.compensarCompraPorFalloInscripcion(evento.getOrdenId(), evento.getEstudianteId(), evento.getCursoId(), evento.getMotivo());
            log.info("component=saga-consumer eventType={} ordenId={} status={}",
                evento.getTipoEvento(), evento.getOrdenId(), aplicada ? "compensated" : "ignored");
        }
    }
}
