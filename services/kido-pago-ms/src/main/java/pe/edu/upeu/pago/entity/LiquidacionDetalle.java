package pe.edu.upeu.pago.entity;
import jakarta.persistence.*;import lombok.*;import java.math.BigDecimal;
@Entity @Table(name="liquidacion_detalles") @Getter @Setter @NoArgsConstructor public class LiquidacionDetalle {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 private Long liquidacionId;private Long movimientoId;@Column(length=300) private String concepto;
 @Column(precision=12,scale=2) private BigDecimal importe;
}
