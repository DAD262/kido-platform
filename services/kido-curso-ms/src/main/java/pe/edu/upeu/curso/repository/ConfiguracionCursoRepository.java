package pe.edu.upeu.curso.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.upeu.curso.entity.ConfiguracionCurso;
import java.util.Optional;
public interface ConfiguracionCursoRepository extends JpaRepository<ConfiguracionCurso,Long>{ Optional<ConfiguracionCurso> findByCursoId(Long cursoId); }
