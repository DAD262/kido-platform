package pe.edu.upeu.pago.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity @Table(name="ordenes_compra")
@Getter @Setter @NoArgsConstructor
public class OrdenCompra {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private AmbientePago ambiente = AmbientePago.DESCONOCIDO;
 @Column(name="receptor_mp",length=40) private String receptorMp;
 private int pagosAprobadosDetectados;
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
 @Column(name="mp_payment_id",length=120) private String mercadoPagoPaymentId;
 @Column(name="compra_carrito_id") private Long compraCarritoId;
 @Column(name="inscripcion_sincronizada",nullable=false) private boolean inscripcionSincronizada;
 // Immutable course snapshot captured BEFORE payment. Avoid depending on Course while confirming money.
 @Column(name="lecciones_snapshot",nullable=false,length=8000) private String leccionesSnapshot = "";
 @Column(name="progreso_minimo_snapshot",nullable=false) private Integer progresoMinimoSnapshot = 100;
 @Column(name="asistencia_minima_snapshot",nullable=false) private Integer asistenciaMinimaSnapshot = 0;
 @Column(name="certificado_habilitado_snapshot",nullable=false) private boolean certificadoHabilitadoSnapshot = false;
 @Column(name="certificado_costo_snapshot",nullable=false,precision=10,scale=2) private BigDecimal certificadoCostoSnapshot = BigDecimal.ZERO;
 @Column(name="fecha_creacion",nullable=false) private LocalDateTime fechaCreacion;
 @Column(name="fecha_aprobacion") private LocalDateTime fechaAprobacion;
 @Column(precision=12,scale=2) private BigDecimal costoProcesamiento;
 @Column(precision=12,scale=2) private BigDecimal netoRecibido;
 @Column(length=40) private String fondosEstado;
 private LocalDateTime fechaConciliacion;
 private LocalDateTime fechaLiberacion;
 private boolean requiereRevocacion;
 public enum EstadoOrden { PENDIENTE, PAGO_INICIADO, APROBADA, RECHAZADA, REEMBOLSADA, CONTRACARGO }
}
