package pe.edu.upeu.curso.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "configuraciones_curso")
@Getter @Setter @NoArgsConstructor
public class ConfiguracionCurso {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name="curso_id", nullable=false, unique=true)
    private Long cursoId;
    @Column(name="progreso_minimo", nullable=false)
    private Integer progresoMinimo = 100;
    @Column(name="asistencia_minima", nullable=false)
    private Integer asistenciaMinima = 0;
    @Column(name="certificado_habilitado", nullable=false)
    private boolean certificadoHabilitado = false;
    @Column(name="certificado_costo", nullable=false, precision=10, scale=2)
    private BigDecimal certificadoCosto = BigDecimal.ZERO;
}
