package pe.edu.upeu.pago.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.upeu.pago.entity.Reembolso;
import java.util.*;
public interface ReembolsoRepository extends JpaRepository<Reembolso,Long>{ Optional<Reembolso> findByOrdenId(Long ordenId); }
