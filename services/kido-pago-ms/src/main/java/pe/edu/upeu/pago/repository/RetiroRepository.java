package pe.edu.upeu.pago.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.upeu.pago.entity.Retiro;
import java.util.List;
public interface RetiroRepository extends JpaRepository<Retiro,Long>{ List<Retiro> findByDocenteIdOrderByIdDesc(Long docenteId); }
