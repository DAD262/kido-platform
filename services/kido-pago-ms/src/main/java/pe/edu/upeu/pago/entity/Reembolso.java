package pe.edu.upeu.pago.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity @Table(name="reembolsos")
@Getter @Setter @NoArgsConstructor
public class Reembolso {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(name="orden_id",nullable=false,unique=true) private Long ordenId;
 @Column(name="estudiante_id",nullable=false) private Long estudianteId;
 @Column(name="curso_id",nullable=false) private Long cursoId;
 @Column(nullable=false,precision=10,scale=2) private BigDecimal monto;
 @Column(nullable=false,length=500) private String motivo;
 @Column(name="porcentaje_progreso",nullable=false) private Integer porcentajeProgreso;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private EstadoReembolso estado;
 @Column(name="fecha_solicitud",nullable=false) private LocalDateTime fechaSolicitud;
 @Column(name="fecha_proceso") private LocalDateTime fechaProceso;
 @Column(name="motivo_rechazo",length=300) private String motivoRechazo;
 public enum EstadoReembolso { SOLICITADO, APROBADO, RECHAZADO }
}
