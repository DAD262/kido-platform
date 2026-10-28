package pe.edu.upeu.pago.config;
import org.springframework.context.annotation.*;import java.time.*;
@Configuration public class FinanzasConfig {@Bean public Clock finanzasClock(){return Clock.system(ZoneId.of("America/Lima"));}}
