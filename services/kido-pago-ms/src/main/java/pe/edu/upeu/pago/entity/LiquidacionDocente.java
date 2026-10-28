package pe.edu.upeu.pago.entity;
import jakarta.persistence.*;import lombok.*;import java.time.*;import java.math.BigDecimal;
@Entity @Table(name="liquidaciones_docente") @Getter @Setter @NoArgsConstructor
public class LiquidacionDocente {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 private Long docenteId;private LocalDate desde;private LocalDate corte;
 @Enumerated(EnumType.STRING) @Column(length=20) private Estado estado;
 @Column(precision=12,scale=2) private BigDecimal monto;
 @Column(precision=12,scale=2) private BigDecimal baseAcumulada;
 @Column(precision=12,scale=2) private BigDecimal compromisosAnteriores;
 private Long movimientoId;private Instant fechaCreacion;private Instant fechaPago;
 @Column(length=120) private String banco;@Column(length=120) private String codigoOperacion;
 private LocalDate fechaOperacion;@Column(length=160) private String destino;
 @Column(length=180) private String titular;@Column(length=100) private String verificadoPor;
 @Column(length=300) private String motivoAnulacion;
 @Column(length=40) private String constanciaTipo;@Column(length=64) private String constanciaSha256;
 public enum Estado {PENDIENTE,PAGADA,ANULADA}
}
