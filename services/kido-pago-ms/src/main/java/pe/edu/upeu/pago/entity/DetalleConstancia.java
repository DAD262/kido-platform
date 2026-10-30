package pe.edu.upeu.pago.entity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
@Embeddable @Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class DetalleConstancia {
 private Long ordenId;
 private Long cursoId;
 private Long docenteId;
 private String titulo;
 private BigDecimal importe;
}
