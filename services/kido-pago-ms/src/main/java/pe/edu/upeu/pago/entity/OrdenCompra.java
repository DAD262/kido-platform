package pe.edu.upeu.pago.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity @Table(name="ordenes_compra")
@Getter @Setter @NoArgsConstructor
public class OrdenCompra {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(name="estudiante_id",nullable=false) private Long estudianteId;
 @Column(name="curso_id",nullable=false) private Long cursoId;
 @Column(name="docente_id",nullable=false) private Long docenteId;
 @Column(name="curso_titulo",nullable=false,length=180) private String cursoTitulo;
 @Column(name="monto_total",nullable=false,precision=10,scale=2) private BigDecimal montoTotal;
 @Column(name="porcentaje_comision",nullable=false,precision=5,scale=2) private BigDecimal porcentajeComision;
 @Column(name="comision_kido",nullable=false,precision=10,scale=2) private BigDecimal comisionKido;
 @Column(name="monto_docente",nullable=false,precision=10,scale=2) private BigDecimal montoDocente;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=24) private EstadoOrden estado;
 @Column(name="mp_preference_id",length=120) private String mercadoPagoPreferenceId;
 @Column(name="mp_payment_id",unique=true,length=120) private String mercadoPagoPaymentId;
 @Column(name="inscripcion_sincronizada",nullable=false) private boolean inscripcionSincronizada;
 @Column(name="fecha_creacion",nullable=false) private LocalDateTime fechaCreacion;
 @Column(name="fecha_aprobacion") private LocalDateTime fechaAprobacion;
 public enum EstadoOrden { PENDIENTE, PAGO_INICIADO, APROBADA, RECHAZADA, REEMBOLSADA }
}
