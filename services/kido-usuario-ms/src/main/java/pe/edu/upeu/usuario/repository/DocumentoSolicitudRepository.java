package pe.edu.upeu.usuario.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.upeu.usuario.entity.DocumentoSolicitud;

public interface DocumentoSolicitudRepository extends JpaRepository<DocumentoSolicitud, Long> {
    Optional<DocumentoSolicitud> findBySolicitudId(Long solicitudId);
}
