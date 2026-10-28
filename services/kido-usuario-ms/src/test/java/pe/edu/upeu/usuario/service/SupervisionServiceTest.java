package pe.edu.upeu.usuario.service;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import pe.edu.upeu.usuario.keycloak.KeycloakAdminClient;
import pe.edu.upeu.usuario.exception.RecursoNoEncontradoException;
class SupervisionServiceTest {
    @Test void impideConsultarClasesNoAsignadasAntesDeContactarServicios(){
        JdbcTemplate jdbc=mock(JdbcTemplate.class);KeycloakAdminClient kc=mock(KeycloakAdminClient.class);
        when(jdbc.queryForList("SELECT curso_id FROM supervision_cursos WHERE supervisor_sub=? ORDER BY curso_id",Long.class,"super")).thenReturn(List.of(2L));
        SupervisionService service=new SupervisionService(jdbc,mock(TransactionTemplate.class),kc,"http://127.0.0.1:1","http://127.0.0.1:1");
        assertThrows(RecursoNoEncontradoException.class,()->service.seguimiento("super",9L));verifyNoInteractions(kc);
    }
}
