package pe.edu.upeu.curso.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.upeu.curso.entity.Material;
import java.util.List;
public interface MaterialRepository extends JpaRepository<Material,Long>{ List<Material> findByLeccionIdOrderByIdAsc(Long leccionId); void deleteByLeccionId(Long leccionId); }
