package pe.edu.upeu.pago.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.upeu.pago.entity.MovimientoSaldo;
import java.time.LocalDateTime;
import java.util.*;
public interface MovimientoSaldoRepository extends JpaRepository<MovimientoSaldo,Long>{
 Optional<MovimientoSaldo> findByOrdenIdAndTipo(Long ordenId, MovimientoSaldo.TipoMovimiento tipo);
 List<MovimientoSaldo> findByDocenteId(Long docenteId);
 List<MovimientoSaldo> findByEstadoAndFechaDisponibleLessThanEqual(MovimientoSaldo.EstadoMovimiento estado, LocalDateTime fecha);
}
