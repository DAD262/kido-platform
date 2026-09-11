package pe.edu.upeu.curso.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "modulos")
@Getter @Setter @NoArgsConstructor
public class Modulo {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "curso_id", nullable = false)
    private Long cursoId;
    @Column(nullable = false, length = 150)
    private String titulo;
    @Column(name = "orden_modulo", nullable = false)
    private Integer orden;
}
