package pe.edu.upeu.curso.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.edu.upeu.curso.entity.Leccion;
import java.util.List;

public interface LeccionRepository extends JpaRepository<Leccion, Long> {
    @Query(value = """
            select l.id
            from lecciones l
            join modulos m on m.id = l.modulo_id
            where m.curso_id = :cursoId
            order by m.orden_modulo, l.orden_leccion, l.id
            """, nativeQuery = true)
    List<Long> findIdsByCursoId(@Param("cursoId") Long cursoId);
}
