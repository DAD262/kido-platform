package pe.edu.upeu.curso.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name="materiales")
@Getter @Setter @NoArgsConstructor
public class Material {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    @Column(name="leccion_id", nullable=false)
    private Long leccionId;
    @Column(nullable=false, length=160)
    private String nombre;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=20)
    private TipoMaterial tipo;
    @Column(nullable=false, length=700)
    private String url;
    @Column(length=500)
    private String descripcion;
    @Column(nullable=false)
    private boolean descargable = true;
    @Column(name="fecha_creacion", nullable=false)
    private LocalDateTime fechaCreacion;
    public enum TipoMaterial { VIDEO, PDF, DOCUMENTO, ENLACE, IMAGEN, AUDIO, OTRO }
}
