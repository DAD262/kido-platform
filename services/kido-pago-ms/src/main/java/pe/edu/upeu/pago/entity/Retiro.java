package pe.edu.upeu.pago.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity @Table(name="retiros")
@Getter @Setter @NoArgsConstructor
public class Retiro {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(name="docente_id",nullable=false) private Long docenteId;
 @Column(nullable=false,precision=10,scale=2) private BigDecimal monto;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private MedioRetiro medio;
 @Column(nullable=false,length=160) private String destino;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private EstadoRetiro estado;
 @Column(name="codigo_operacion",length=120) private String codigoOperacion;
 @Column(name="motivo_rechazo",length=300) private String motivoRechazo;
 @Column(name="movimiento_id") private Long movimientoId;
 @Column(name="fecha_solicitud",nullable=false) private LocalDateTime fechaSolicitud;
 @Column(name="fecha_proceso") private LocalDateTime fechaProceso;
 public enum MedioRetiro { CUENTA_BANCARIA, YAPE, PLIN }
 public enum EstadoRetiro { PENDING, APPROVED, PAID, REJECTED }
}
