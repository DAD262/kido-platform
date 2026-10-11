package pe.edu.upeu.notificacion.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagoAprobadoEvento {
    private String tipoEvento;
    private Long ordenId;
    private Long estudianteId;
    private Long docenteId;
    private Long cursoId;
    private String cursoTitulo;
    private BigDecimal montoTotal;
    private BigDecimal montoDocente;
    private Long diasRetencion;
    private String estado;
    private String origen;
    private Long timestamp;
}
