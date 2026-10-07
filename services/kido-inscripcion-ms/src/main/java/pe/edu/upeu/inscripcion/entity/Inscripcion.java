package pe.edu.upeu.inscripcion.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name="inscripciones", uniqueConstraints=@UniqueConstraint(columnNames={"estudiante_id","curso_id"}))
@Getter @Setter @NoArgsConstructor
public class Inscripcion {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    @Column(name="estudiante_id", nullable=false)
    private Long estudianteId;
    @Column(name="curso_id", nullable=false)
    private Long cursoId;
    @Enumerated(EnumType.STRING) @Column(name="tipo_acceso",nullable=false,length=15)
    private TipoAcceso tipoAcceso;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=15)
    private EstadoInscripcion estado;
    @Column(name="fecha_inscripcion",nullable=false)
    private LocalDateTime fechaInscripcion;
    @Column(name="progreso_minimo",nullable=false) private Integer progresoMinimo = 100;
    @Column(name="asistencia_minima",nullable=false) private Integer asistenciaMinima = 0;
    @Column(name="certificado_habilitado",nullable=false) private boolean certificadoHabilitado = false;
    @Column(name="certificado_costo",nullable=false,precision=10,scale=2) private java.math.BigDecimal certificadoCosto = java.math.BigDecimal.ZERO;
    @OneToMany(mappedBy="inscripcion",cascade=CascadeType.ALL,orphanRemoval=true)
    @OrderBy("id ASC")
    private List<ProgresoLeccion> progresos = new ArrayList<>();

    public enum TipoAcceso { GRATUITO, COMPRA }
    public enum EstadoInscripcion { ACTIVA, COMPLETADA, REVOCADA, CANCELADA }
}
