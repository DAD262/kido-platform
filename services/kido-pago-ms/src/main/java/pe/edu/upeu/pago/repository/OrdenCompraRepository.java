package pe.edu.upeu.pago.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.upeu.pago.entity.OrdenCompra;
import java.util.*;
public interface OrdenCompraRepository extends JpaRepository<OrdenCompra,Long>{
 @org.springframework.data.jpa.repository.Query("select o from OrdenCompra o where o.mercadoPagoPaymentId=:id and o.compraCarritoId is null") Optional<OrdenCompra> findByMercadoPagoPaymentId(@org.springframework.data.repository.query.Param("id") String id);
 List<OrdenCompra> findByCompraCarritoIdOrderByIdAsc(Long id);
 List<OrdenCompra> findByDocenteIdOrderByIdDesc(Long docenteId);
 List<OrdenCompra> findByEstudianteIdOrderByIdDesc(Long estudianteId);
 Optional<OrdenCompra> findFirstByEstudianteIdAndCursoIdAndEstadoInOrderByIdDesc(Long estudianteId, Long cursoId, Collection<OrdenCompra.EstadoOrden> estados);
 boolean existsByEstudianteIdAndCursoIdAndEstado(Long estudianteId, Long cursoId, OrdenCompra.EstadoOrden estado);
 @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
 @org.springframework.data.jpa.repository.Query("select o from OrdenCompra o where o.id=:id") java.util.Optional<OrdenCompra> bloquear(@org.springframework.data.repository.query.Param("id") Long id);
 @org.springframework.data.jpa.repository.Query("select o.id from OrdenCompra o where o.ambiente=pe.edu.upeu.pago.entity.AmbientePago.PRODUCCION and o.estado=:estado and o.mercadoPagoPaymentId is not null and o.mercadoPagoPaymentId not like 'SIM-%' order by case when o.fechaConciliacion is null then 0 else 1 end,o.fechaConciliacion") java.util.List<Long> porConciliar(@org.springframework.data.repository.query.Param("estado") OrdenCompra.EstadoOrden estado,org.springframework.data.domain.Pageable pagina);
 java.util.List<OrdenCompra> findByRequiereRevocacionTrue(org.springframework.data.domain.Pageable pagina);
 @org.springframework.data.jpa.repository.Query("select o.docenteId from OrdenCompra o where o.id=:id") java.util.Optional<Long> docenteDe(@org.springframework.data.repository.query.Param("id") Long id);
}
