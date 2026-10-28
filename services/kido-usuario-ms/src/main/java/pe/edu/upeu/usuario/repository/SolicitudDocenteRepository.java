package pe.edu.upeu.usuario.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.upeu.usuario.entity.EstadoSolicitud;
import pe.edu.upeu.usuario.entity.SolicitudDocente;

public interface SolicitudDocenteRepository extends JpaRepository<SolicitudDocente, Long> {
    Optional<SolicitudDocente> findByPerfilId(Long perfilId);

    List<SolicitudDocente> findByEstadoInOrderByEnviadoEnAsc(Collection<EstadoSolicitud> estados);

    List<SolicitudDocente> findAllByOrderByEnviadoEnDesc();

    long countByEstado(EstadoSolicitud estado);

    boolean existsByDniAndEstadoNotAndPerfilIdNot(String dni, EstadoSolicitud estado, Long perfilId);

    boolean existsByDniAndEstadoNot(String dni, EstadoSolicitud estado);
}
