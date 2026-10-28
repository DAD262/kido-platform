package pe.edu.upeu.inscripcion.client;
import static org.junit.jupiter.api.Assertions.*;import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import org.junit.jupiter.api.*;import org.springframework.test.util.ReflectionTestUtils;import org.springframework.test.web.client.MockRestServiceServer;import org.springframework.web.client.RestClient;import org.springframework.http.MediaType;import org.springframework.web.server.ResponseStatusException;
class CertificadoContextoClientTest {
 CertificadoContextoClient client;MockRestServiceServer server;
 @BeforeEach void setup(){var builder=RestClient.builder();server=MockRestServiceServer.bindTo(builder).build();client=new CertificadoContextoClient(builder.build());ReflectionTestUtils.setField(client,"usuarios","http://cuentas.local");}
 void perfil(){server.expect(requestTo("http://cuentas.local/api/v1/usuarios/me")).andExpect(header("Authorization","Bearer prueba")).andRespond(withSuccess("{\"id\":1001,\"nombres\":\"María\",\"apellidos\":\"Álvarez\",\"roles\":[\"ESTUDIANTE\"]}",MediaType.APPLICATION_JSON));}
 @Test void usaNombreDelPerfilAutenticado(){perfil();assertEquals("María Álvarez",client.alumno(1001L,"Bearer prueba"));server.verify();}
 @Test void rechazaDescargaDeOtroEstudiante(){perfil();assertThrows(ResponseStatusException.class,()->client.propietario(99L,"Bearer prueba"));server.verify();}
 @Test void noHayAccesoAnonimo(){assertThrows(ResponseStatusException.class,()->client.cuenta(null));server.verify();}
 @Test void resumenCursoAceptaLosCamposAdicionalesDelCatalogo(){ReflectionTestUtils.setField(client,"catalogo","http://cursos.local");server.expect(requestTo("http://cursos.local/internal/v1/cursos/3/resumen-compra")).andRespond(withSuccess("{\"id\":3,\"titulo\":\"Python\",\"tipo\":\"GRATUITO\",\"precio\":0,\"estado\":\"PUBLICADO\",\"docenteId\":1000,\"leccionIds\":[6],\"progresoMinimo\":100,\"asistenciaMinima\":0,\"certificadoHabilitado\":true,\"certificadoCosto\":0}",MediaType.APPLICATION_JSON));var c=client.curso(3L);assertEquals("Python",c.titulo());assertTrue(c.certificadoHabilitado());server.verify();}
}
