package pe.edu.upeu.curso.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "lecciones")
@Getter @Setter @NoArgsConstructor
public class Leccion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "modulo_id", nullable = false)
    private Long moduloId;
    @Column(nullable = false, length = 150)
    private String titulo;
    @Column(name = "orden_leccion", nullable = false)
    private Integer orden;
    @Column(name = "url_contenido", length = 500)
    private String urlContenido;
}
