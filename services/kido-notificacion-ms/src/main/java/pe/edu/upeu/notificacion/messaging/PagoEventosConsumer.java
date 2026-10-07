package pe.edu.upeu.notificacion.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import pe.edu.upeu.notificacion.dto.NotificacionRequest;
import pe.edu.upeu.notificacion.event.PagoAprobadoEvento;
import pe.edu.upeu.notificacion.service.NotificacionService;

@Slf4j
@Component
@RequiredArgsConstructor
public class PagoEventosConsumer {
    private static final String PAGO_APROBADO = "pago.aprobado";

    private final NotificacionService notificacionService;

    @Value("${app.kafka.topic.pagos}")
    private String topicPagos;

    @KafkaListener(topics = "${app.kafka.topic.pagos}")
    public void alRecibirPago(PagoAprobadoEvento evento) {
        if (!PAGO_APROBADO.equals(evento.getTipoEvento())) {
            log.warn("component=consumer eventType={} ordenId={} status=ignored",
                evento.getTipoEvento(), evento.getOrdenId());
            return;
        }

        log.info("component=consumer topic={} eventType={} ordenId={} status=consumed",
            topicPagos, evento.getTipoEvento(), evento.getOrdenId());

        notificacionService.crear(new NotificacionRequest(
            evento.getEstudianteId(),
            "PAGO_APROBADO",
            "IN_APP",
            "Pago aprobado",
            "Tu compra de " + evento.getCursoTitulo() + " fue aprobada.",
            null
        ));

        notificacionService.crear(new NotificacionRequest(
            evento.getDocenteId(),
            "NUEVA_VENTA",
            "IN_APP",
            "Nueva venta",
            "Tienes una nueva venta. Tu saldo neto es S/ " + evento.getMontoDocente()
                + " y se libera en " + evento.getDiasRetencion() + " días.",
            null
        ));

        log.info("component=processor eventType={} ordenId={} notifications=2 status=processed",
            evento.getTipoEvento(), evento.getOrdenId());
    }

    @KafkaListener(topics = "${app.kafka.topic.academico:kido-academico-eventos}")
    public void alRecibirAcademico(PagoAprobadoEvento evento) {
        if ("curso.completado".equals(evento.getTipoEvento())) {
            notificacionService.crear(new NotificacionRequest(evento.getEstudianteId(), "CURSO_COMPLETADO", "IN_APP", "Curso completado", "Cumpliste los requisitos académicos del curso #" + evento.getCursoId() + ".", null));
        } else if ("certificado.disponible".equals(evento.getTipoEvento())) {
            notificacionService.crear(new NotificacionRequest(evento.getEstudianteId(), "CERTIFICADO_DISPONIBLE", "IN_APP", "Certificado disponible", "Tu certificado del curso #" + evento.getCursoId() + " ya está disponible para descargar.", null));
        } else {
            log.debug("Evento académico ignorado: {}", evento.getTipoEvento());
        }
    }
}
