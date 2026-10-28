package pe.edu.upeu.pago.entity;
import jakarta.persistence.*;import lombok.*;
@Entity @Table(name="constancias_liquidacion") @Getter @Setter @NoArgsConstructor public class ConstanciaLiquidacion {
 @Id private Long liquidacionId;
 @Column(nullable=false,columnDefinition="bytea") private byte[] contenido;
}
