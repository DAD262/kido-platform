package pe.edu.upeu.curso.repository;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import pe.edu.upeu.curso.entity.Curso;
import java.util.*;

public interface CursoRepository extends JpaRepository<Curso, Long> {
    @Query("select c from Curso c join fetch c.categoria order by c.id")
    List<Curso> findAllConCategoria();

    @Query("select c from Curso c join fetch c.categoria where c.id=:id")
    Optional<Curso> findByIdConCategoria(@Param("id") Long id);
}
