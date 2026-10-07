package pe.edu.upeu.pago.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.upeu.pago.entity.OrdenCompra;
import java.util.*;
public interface OrdenCompraRepository extends JpaRepository<OrdenCompra,Long>{
 Optional<OrdenCompra> findByMercadoPagoPaymentId(String id);
 List<OrdenCompra> findByDocenteIdOrderByIdDesc(Long docenteId);
 List<OrdenCompra> findByEstudianteIdOrderByIdDesc(Long estudianteId);
 Optional<OrdenCompra> findFirstByEstudianteIdAndCursoIdAndEstadoInOrderByIdDesc(Long estudianteId, Long cursoId, Collection<OrdenCompra.EstadoOrden> estados);
}
