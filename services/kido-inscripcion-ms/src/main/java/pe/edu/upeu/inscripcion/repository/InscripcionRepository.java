package pe.edu.upeu.inscripcion.repository;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import pe.edu.upeu.inscripcion.entity.Inscripcion;
import java.util.*;

public interface InscripcionRepository extends JpaRepository<Inscripcion,Long> {
    @Query("select distinct i from Inscripcion i left join fetch i.progresos order by i.id")
    List<Inscripcion> findAllConProgresos();
    @Query("select distinct i from Inscripcion i left join fetch i.progresos where i.id=:id")
    Optional<Inscripcion> findByIdConProgresos(@Param("id") Long id);
    boolean existsByEstudianteIdAndCursoId(Long estudianteId, Long cursoId);
    @Query("select distinct i from Inscripcion i left join fetch i.progresos where i.estudianteId=:estudianteId and i.cursoId=:cursoId")
    Optional<Inscripcion> findByEstudianteIdAndCursoIdConProgresos(@Param("estudianteId") Long estudianteId, @Param("cursoId") Long cursoId);
}
