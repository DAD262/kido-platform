package pe.edu.upeu.pago.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.util.*;

@Entity @Table(name="carritos_estudiante") @Getter @Setter
public class CarritoEstudiante {
 @Id private Long estudianteId;
 @Version private long version;
 @ElementCollection(fetch=FetchType.EAGER)
 @CollectionTable(name="carrito_cursos",joinColumns=@JoinColumn(name="estudiante_id"))
 @Column(name="curso_id") private Set<Long> cursos=new LinkedHashSet<>();
}
