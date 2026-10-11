package pe.edu.upeu.inscripcion.messaging;

import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import pe.edu.upeu.inscripcion.dto.CompraInscripcionRequest;
import pe.edu.upeu.inscripcion.event.AcademicoEvento;
import pe.edu.upeu.inscripcion.event.PagoCompraEvento;
import pe.edu.upeu.inscripcion.exception.InscripcionSagaRechazadaException;
import pe.edu.upeu.inscripcion.service.InscripcionService;

@Slf4j
@Component
@RequiredArgsConstructor
public class PagoCompraEventosConsumer {
    private static final String APROBADO = "pago.aprobado";
    private final InscripcionService inscripcionService;
    private final AcademicoEventosProducer productor;

    // Enabled only by explicit local configuration; never hard-coded in production.
    @Value("${kido.saga.estudiante-fallo-demo:0}") private long estudianteFalloDemo;

    @KafkaListener(topics = "${app.kafka.topic.pagos}", groupId = "kido-inscripcion-saga-ms")
    public void alRecibirPago(PagoCompraEvento evento) {
        if (!APROBADO.equals(evento.getTipoEvento())) return;
        // Ignore legacy S08 messages when a brand-new consumer group reads historical Kafka data.
        if (!Integer.valueOf(2).equals(evento.getSchemaVersion())) {
            log.info("component=saga-consumer ordenId={} status=ignored motivo=evento-S08-sin-version-Saga",
                evento.getOrdenId());
            return;
        }
        if (evento.getOrdenId() == null || evento.getEstudianteId() == null || evento.getCursoId() == null
                || evento.getOrdenId() <= 0 || evento.getEstudianteId() <= 0 || evento.getCursoId() <= 0
                || !"kido-pago-ms".equals(evento.getOrigen())) {
            throw new IllegalArgumentException("Evento pago.aprobado incompleto o de origen inesperado");
        }
        if (!"APROBADA".equals(evento.getEstado())) {
            log.info("component=saga-consumer ordenId={} status=ignored motivo=estado-no-aprobado", evento.getOrdenId());
            return;
        }
        log.info("component=saga-consumer eventType={} ordenId={} status=consumed", evento.getTipoEvento(), evento.getOrdenId());
        try {
            if (estudianteFalloDemo > 0 && evento.getEstudianteId() == estudianteFalloDemo) {
                throw new InscripcionSagaRechazadaException("Fallo de inscripción simulado para S09");
            }
            boolean creada = inscripcionService.crearPorCompraIdempotente(evento.getOrdenId(),
                new CompraInscripcionRequest(evento.getEstudianteId(), evento.getCursoId(), evento.getLeccionIds(),
                    evento.getProgresoMinimo(), evento.getAsistenciaMinima(),
                    evento.getCertificadoHabilitado(), evento.getCertificadoCosto()));
            publicarResultado(evento, "inscripcion.activada", "ACTIVA", null);
            log.info("component=saga-processor ordenId={} status={}", evento.getOrdenId(), creada ? "processed" : "ignored");
        } catch (InscripcionSagaRechazadaException ex) {
            log.warn("component=saga-processor ordenId={} status=compensation-requested motivo={}",
                evento.getOrdenId(), ex.getMessage());
            publicarResultado(evento, "inscripcion.fallida", "FALLIDA", ex.getMessage());
        } catch (DataIntegrityViolationException ex) {
            // A UNIQUE conflict is an acknowledged duplicate ONLY if this precise order exists.
            if (!inscripcionService.existeCompraDeOrden(evento.getOrdenId())) {
                if (inscripcionService.existeCompraActivaDeOtraOrden(
                        evento.getEstudianteId(), evento.getCursoId(), evento.getOrdenId())) {
                    publicarResultado(evento, "inscripcion.fallida", "FALLIDA",
                        "Ya existe una compra activa para el estudiante y curso");
                    log.warn("component=saga-processor ordenId={} status=compensation-requested motivo=UNIQUE-otra-orden",
                        evento.getOrdenId());
                    return;
                }
                log.error("component=saga-processor ordenId={} status=retry motivo=violacion-no-duplicada",
                    evento.getOrdenId(), ex);
                throw ex;
            }
            publicarResultado(evento, "inscripcion.activada", "ACTIVA", null);
            log.info("component=saga-processor ordenId={} status=ignored motivo=UNIQUE", evento.getOrdenId());
        } catch (RuntimeException ex) {
            // Database outage, Kafka outage, etc.: NEVER issue an automatic refund for a transient failure.
            log.error("component=saga-processor ordenId={} status=retry error={}", evento.getOrdenId(), ex.toString());
            throw ex;
        }
    }

    private void publicarResultado(PagoCompraEvento e, String tipo, String estado, String motivo) {
        productor.publicarConfirmado(AcademicoEvento.builder().tipoEvento(tipo)
            .ordenId(e.getOrdenId()).estudianteId(e.getEstudianteId()).cursoId(e.getCursoId())
            .estado(estado).origen("kido-inscripcion-ms").motivo(motivo)
            .timestamp(Instant.now().toEpochMilli()).build());
    }
}
