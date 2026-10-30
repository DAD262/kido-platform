package pe.edu.upeu.pago.entity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity @Table(name="constancias_compra") @Getter @Setter @NoArgsConstructor
public class ConstanciaCompra {
 private Long ordenId;
 private Long compraCarritoId;
 @ElementCollection(fetch=FetchType.EAGER) @CollectionTable(name="constancia_detalles",joinColumns=@JoinColumn(name="operacion")) @OrderBy("ordenId ASC")
 private java.util.List<DetalleConstancia> detalles=new java.util.ArrayList<>();
 private String numero;
 @Id private String operacion;
 private String ambiente;
 private String alumnoNombre;
 private String alumnoEmail;
 private String alumnoDocumento;
 private String cursoTitulo;
 private Long docenteId;
 private LocalDateTime fechaPago;
 private String medioPago;
 private String detalleEstado;
 private BigDecimal total;
 private String soporte;
 private LocalDateTime generadaEn;
}
