package pe.edu.upeu.pago.repository;
import org.springframework.data.jpa.repository.JpaRepository; import pe.edu.upeu.pago.entity.AceptacionCompra;
public interface AceptacionCompraRepository extends JpaRepository<AceptacionCompra,Long> {
 boolean existsByTipoAndOrdenIdAndSujetoAndVersionComercial(String tipo,Long ordenId,String sujeto,Long versionComercial);
}
