package pe.edu.upeu.pago.repository;
import pe.edu.upeu.pago.entity.CompraCarrito;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.*;
public interface CompraCarritoRepository extends JpaRepository<CompraCarrito,Long> {
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select c from CompraCarrito c where c.id=:id") Optional<CompraCarrito> bloquear(@Param("id") Long id);
 List<CompraCarrito> findByEstudianteIdAndHuellaOrderByIdDesc(Long estudiante,String huella);
 Optional<CompraCarrito> findByMercadoPagoPaymentId(String pago);
}
