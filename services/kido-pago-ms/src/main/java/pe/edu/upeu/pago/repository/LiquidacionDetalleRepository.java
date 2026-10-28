package pe.edu.upeu.pago.repository;
import org.springframework.data.jpa.repository.JpaRepository;import java.util.List;import pe.edu.upeu.pago.entity.LiquidacionDetalle;
public interface LiquidacionDetalleRepository extends JpaRepository<LiquidacionDetalle,Long>{List<LiquidacionDetalle> findByLiquidacionIdOrderById(Long id);}
