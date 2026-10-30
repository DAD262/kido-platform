package pe.edu.upeu.pago.repository;
import org.springframework.data.jpa.repository.*;import org.springframework.data.repository.query.Param;import jakarta.persistence.LockModeType;import java.util.*;import java.time.LocalDate;import pe.edu.upeu.pago.entity.LiquidacionDocente;
public interface LiquidacionRepository extends JpaRepository<LiquidacionDocente,Long>{
 List<LiquidacionDocente> findAllByOrderByIdDesc();List<LiquidacionDocente> findByDocenteIdOrderByIdDesc(Long id);
 boolean existsByDocenteIdAndCorteAndEstadoNot(Long id,LocalDate corte,LiquidacionDocente.Estado estado);
 List<LiquidacionDocente> findByDocenteIdAndEstado(Long id,LiquidacionDocente.Estado estado);
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select l from LiquidacionDocente l where l.id=:id") Optional<LiquidacionDocente> bloquear(@Param("id")Long id);
 boolean existsByBancoAndFechaOperacionAndCodigoOperacionAndEstado(String banco,LocalDate fecha,String codigo,LiquidacionDocente.Estado estado);
 @Query("select l.docenteId from LiquidacionDocente l where l.id=:id") Optional<Long> docenteDe(@Param("id") Long id);
 boolean existsByConstanciaSha256AndEstado(String hash,LiquidacionDocente.Estado estado);
}