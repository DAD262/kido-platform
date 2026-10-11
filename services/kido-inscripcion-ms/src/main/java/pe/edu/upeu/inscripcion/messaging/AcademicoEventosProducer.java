package pe.edu.upeu.inscripcion.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import java.util.concurrent.TimeUnit;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import pe.edu.upeu.inscripcion.event.AcademicoEvento;

@Slf4j
@Component
@RequiredArgsConstructor
public class AcademicoEventosProducer {
    private final KafkaTemplate<String, Object> kafka;

    @Value("${app.kafka.topic.academico:kido-academico-eventos}")
    private String topic;

    public void publicarTrasCommit(AcademicoEvento evento) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publicar(evento);
                }
            });
        } else {
            publicar(evento);
        }
    }

    /** Used by the Saga listener: do not ACK the payment before its result reaches Kafka. */
    public void publicarConfirmado(AcademicoEvento evento) {
        String key = evento.getOrdenId() != null
            ? String.valueOf(evento.getOrdenId()) : String.valueOf(evento.getEstudianteId());
        try {
            var resultado = kafka.send(topic, key, evento).get(15, TimeUnit.SECONDS);
            log.info("component=producer topic={} partition={} offset={} eventType={} ordenId={} status=published",
                topic, resultado.getRecordMetadata().partition(), resultado.getRecordMetadata().offset(),
                evento.getTipoEvento(), evento.getOrdenId());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Publicación interrumpida", e);
        } catch (java.util.concurrent.ExecutionException | java.util.concurrent.TimeoutException e) {
            throw new IllegalStateException("No se pudo publicar el resultado de la Saga en Kafka", e);
        }
    }

    public void publicar(AcademicoEvento evento) {
        String key = evento.getOrdenId() != null
            ? String.valueOf(evento.getOrdenId())
            : String.valueOf(evento.getEstudianteId());
        kafka.send(topic, key, evento).whenComplete((resultado, ex) -> {
            if (ex != null) {
                log.error("component=producer topic={} eventType={} ordenId={} status=error error=\"{}\"",
                    topic, evento.getTipoEvento(), evento.getOrdenId(), ex.getMessage());
                return;
            }
            log.info("component=producer topic={} partition={} offset={} eventType={} ordenId={} status=published",
                resultado.getRecordMetadata().topic(),
                resultado.getRecordMetadata().partition(),
                resultado.getRecordMetadata().offset(),
                evento.getTipoEvento(), evento.getOrdenId());
        });
    }
}
