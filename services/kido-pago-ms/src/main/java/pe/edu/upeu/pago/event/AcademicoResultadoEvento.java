package pe.edu.upeu.pago.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AcademicoResultadoEvento {
    private String tipoEvento;
    private Long ordenId;
    private Long estudianteId;
    private Long cursoId;
    private String estado;
    private String motivo;
    private String origen;
    private Long timestamp;
}
