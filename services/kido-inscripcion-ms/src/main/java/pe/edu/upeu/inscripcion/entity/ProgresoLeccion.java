package pe.edu.upeu.inscripcion.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name="progresos_leccion")
@Getter @Setter @NoArgsConstructor
public class ProgresoLeccion {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false)
    @JoinColumn(name="inscripcion_id",nullable=false)
    private Inscripcion inscripcion;
    @Column(name="leccion_id",nullable=false)
    private Long leccionId;
    @Column(nullable=false)
    private boolean completada;
    @Column(name="fecha_completada")
    private LocalDateTime fechaCompletada;
}
