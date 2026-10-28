package pe.edu.upeu.usuario.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.upeu.usuario.entity.Perfil;

public interface PerfilRepository extends JpaRepository<Perfil, Long> {
    Optional<Perfil> findByKeycloakId(String keycloakId);

    Optional<Perfil> findByUsernameIgnoreCaseAndKeycloakIdIsNull(String username);

    Optional<Perfil> findByUsernameIgnoreCase(String username);

    Optional<Perfil> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndKeycloakIdNot(String email, String keycloakId);

    boolean existsByDni(String dni);

    boolean existsByDniAndKeycloakIdNot(String dni, String keycloakId);

    boolean existsByUsernameIgnoreCase(String username);
}
