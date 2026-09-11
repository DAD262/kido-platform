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
    @OneToMany(mappedBy="inscripcion",cascade=CascadeType.ALL,orphanRemoval=true)
    @OrderBy("id ASC")
    private List<ProgresoLeccion> progresos = new ArrayList<>();

    public enum TipoAcceso { GRATUITO, COMPRA }
    public enum EstadoInscripcion { ACTIVA, COMPLETADA, REVOCADA, CANCELADA }
}
