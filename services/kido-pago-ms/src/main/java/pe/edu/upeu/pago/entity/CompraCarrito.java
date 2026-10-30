package pe.edu.upeu.pago.entity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Entity @Table(name="compras_carrito") @Getter @Setter @NoArgsConstructor
public class CompraCarrito {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 private Long estudianteId;
 @Enumerated(EnumType.STRING) private AmbientePago ambiente=AmbientePago.DESCONOCIDO;
 private String receptorMp;
 private BigDecimal total;
 private String huella;
 private String estado;
 private LocalDateTime fechaCreacion;
 @Column(name="mp_preference_id") private String mercadoPagoPreferenceId;
 @Column(name="mp_payment_id") private String mercadoPagoPaymentId;
 private String checkoutUrl;
 private boolean retornoConfigurado;
 private int pagosAprobadosDetectados;
}
