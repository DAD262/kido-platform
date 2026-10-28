package pe.edu.upeu.usuario.service;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import pe.edu.upeu.usuario.config.RecuperacionConfig;
import pe.edu.upeu.usuario.controller.RecuperacionController;
import pe.edu.upeu.usuario.keycloak.KeycloakAdminClient;
import pe.edu.upeu.usuario.repository.PerfilRepository;
class RecuperacionContextTest {
    @Test void iniciaConRelojYDependenciasCompletas() {
        new ApplicationContextRunner().withUserConfiguration(RecuperacionConfig.class,RecuperacionService.class,RecuperacionController.class)
            .withBean(JdbcTemplate.class,()->mock(JdbcTemplate.class))
            .withBean(TransactionTemplate.class,()->mock(TransactionTemplate.class))
            .withBean(KeycloakAdminClient.class,()->mock(KeycloakAdminClient.class))
            .withBean(CorreoRecuperacion.class,()->mock(CorreoRecuperacion.class))
            .withBean(PerfilRepository.class,()->mock(PerfilRepository.class))
            .withPropertyValues("kido.recuperacion.secret=un-secreto-local-de-pruebas-de-32-caracteres", "spring.mail.host=kido-mailpit")
            .run(ctx->{assertThat(ctx).hasNotFailed();assertThat(ctx).hasSingleBean(java.time.Clock.class);assertThat(ctx).hasSingleBean(RecuperacionController.class);});
    }
}
