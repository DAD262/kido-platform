package pe.edu.upeu.pago.repository;
import org.springframework.data.jpa.repository.*;import org.springframework.data.repository.query.Param;import jakarta.persistence.LockModeType;import java.util.Optional;import pe.edu.upeu.pago.entity.BloqueoSaldo;
public interface BloqueoSaldoRepository extends JpaRepository<BloqueoSaldo,Long>{
 @Modifying @Query(value="INSERT INTO saldo_docente_bloqueos(docente_id) VALUES(:id) ON CONFLICT DO NOTHING",nativeQuery=true) void asegurar(@Param("id") Long id);
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select b from BloqueoSaldo b where b.docenteId=:id") Optional<BloqueoSaldo> bloquear(@Param("id") Long id);
}
