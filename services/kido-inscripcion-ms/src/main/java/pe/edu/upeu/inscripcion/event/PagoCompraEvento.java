package pe.edu.upeu.inscripcion.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagoCompraEvento {
    private String tipoEvento;
    /** V2 distinguishes the Saga workflow from S08 historical events. */
    private Integer schemaVersion;
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
    private List<Long> leccionIds;
    private Integer progresoMinimo;
    private Integer asistenciaMinima;
    private Boolean certificadoHabilitado;
    private BigDecimal certificadoCosto;
}
