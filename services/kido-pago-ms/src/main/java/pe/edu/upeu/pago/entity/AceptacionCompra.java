package pe.edu.upeu.pago.entity;
import jakarta.persistence.*; import lombok.*; import java.time.Instant;
@Entity @Table(name="aceptaciones_compra") @Getter @Setter @NoArgsConstructor
public class AceptacionCompra {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false,length=20) private String tipo;
 @Column(nullable=false) private Long ordenId;
 @Column(nullable=false) private Long estudianteId;
 @Column(nullable=false,length=100) private String sujeto;
 @Column(nullable=false) private Long versionComercial;
 @Column(nullable=false) private Instant fecha;
 @Column(nullable=false,columnDefinition="text") private String textoAceptado;
}
