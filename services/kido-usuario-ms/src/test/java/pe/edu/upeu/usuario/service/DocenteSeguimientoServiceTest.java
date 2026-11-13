package pe.edu.upeu.usuario.service;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import java.util.*;
import java.sql.ResultSet;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.*;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import pe.edu.upeu.usuario.exception.RecursoNoEncontradoException;

class DocenteSeguimientoServiceTest {
 JdbcTemplate jdbc;DocenteSeguimientoService service;MockRestServiceServer server;
 @BeforeEach void preparar(){jdbc=mock(JdbcTemplate.class);service=new DocenteSeguimientoService(jdbc,"https://interno.test","https://interno.test","https://interno.test");var builder=RestClient.builder().baseUrl("https://interno.test");server=MockRestServiceServer.bindTo(builder).build();var client=builder.build();for(String campo:List.of("cursos","inscripciones","pagos"))ReflectionTestUtils.setField(service,campo,client);}
 void perfil(){when(jdbc.queryForList("SELECT id FROM perfiles WHERE keycloak_id=?",Long.class,"sub-docente")).thenReturn(List.of(1L));}
 @Test void soloIncluyeCursosYEstudiantesDelDocenteAutenticado() throws Exception {
  perfil();
  server.expect(requestTo("https://interno.test/internal/v1/cursos/docente/1")).andRespond(withSuccess("[{\"id\":11,\"titulo\":\"Java\",\"docenteId\":1},{\"id\":12,\"titulo\":\"Ajeno\",\"docenteId\":2}]",MediaType.APPLICATION_JSON));
  server.expect(requestTo("https://interno.test/internal/v1/inscripciones/curso/11")).andRespond(withSuccess("[{\"id\":3,\"cursoId\":11,\"estudianteId\":7},{\"id\":4,\"cursoId\":12,\"estudianteId\":99}]",MediaType.APPLICATION_JSON));
  server.expect(requestTo("https://interno.test/api/v1/pagos/docentes/1/historial")).andExpect(header("Authorization","Bearer prueba")).andRespond(withSuccess("[{\"id\":5,\"cursoId\":11,\"docenteId\":1,\"estudianteId\":7},{\"id\":6,\"docenteId\":2,\"estudianteId\":99}]",MediaType.APPLICATION_JSON));
  doAnswer(inv->{var rs=mock(ResultSet.class);when(rs.getString("nombre")).thenReturn("Ana Torres");when(rs.getLong("id")).thenReturn(7L);((RowCallbackHandler)inv.getArgument(1)).processRow(rs);return null;}).when(jdbc).query(startsWith("SELECT id,TRIM"),any(RowCallbackHandler.class),any(Object[].class));
  var r=service.consultar("sub-docente","Bearer prueba");assertEquals(1,r.inscripciones().size());assertEquals("Ana Torres",r.inscripciones().getFirst().get("estudianteNombre"));assertEquals("Java",r.inscripciones().getFirst().get("cursoTitulo"));assertEquals(1,r.compras().size());assertTrue(r.fallos().isEmpty());server.verify();
 }
 @Test void noConsultaDatosSinPerfil(){when(jdbc.queryForList("SELECT id FROM perfiles WHERE keycloak_id=?",Long.class,"sin-perfil")).thenReturn(List.of());assertThrows(RecursoNoEncontradoException.class,()->service.consultar("sin-perfil","Bearer prueba"));server.verify();}
 @Test void fallosDeDependenciasNoSePresentanComoListasVaciasSinAviso(){perfil();server.expect(requestTo("https://interno.test/internal/v1/cursos/docente/1")).andRespond(withSuccess("[{\"id\":11,\"titulo\":\"Java\",\"docenteId\":1}]",MediaType.APPLICATION_JSON));server.expect(requestTo("https://interno.test/internal/v1/inscripciones/curso/11")).andRespond(withServerError());server.expect(requestTo("https://interno.test/api/v1/pagos/docentes/1/historial")).andRespond(withServerError());var r=service.consultar("sub-docente","Bearer prueba");assertEquals(2,r.fallos().size());assertTrue(r.inscripciones().isEmpty());server.verify();}
}
