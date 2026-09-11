package pe.edu.upeu.pago.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity @Table(name="movimientos_saldo")
@Getter @Setter @NoArgsConstructor
public class MovimientoSaldo {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(name="docente_id",nullable=false) private Long docenteId;
 @Column(name="orden_id") private Long ordenId;
 @Column(name="retiro_id") private Long retiroId;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private TipoMovimiento tipo;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private EstadoMovimiento estado;
 @Column(nullable=false,precision=10,scale=2) private BigDecimal monto;
 @Column(name="fecha_disponible") private LocalDateTime fechaDisponible;
 @Column(name="fecha_creacion",nullable=false) private LocalDateTime fechaCreacion;
 @Column(length=300) private String descripcion;
 public enum TipoMovimiento { VENTA, RETIRO }
 public enum EstadoMovimiento { PENDIENTE, DISPONIBLE, RESERVADO, PAGADO, REVERTIDO }
}
