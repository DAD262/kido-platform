package pe.edu.upeu.curso.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.upeu.curso.entity.Modulo;
import java.util.List;
public interface ModuloRepository extends JpaRepository<Modulo,Long>{ List<Modulo> findByCursoIdOrderByOrdenAscIdAsc(Long cursoId); }
