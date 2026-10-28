package pe.edu.upeu.usuario.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;


import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class DeseadosServiceTest {
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private RestClient.Builder builder;
    private MockRestServiceServer mockCatalogo;
    private DeseadosService servicio;

    @BeforeEach
    void preparar() {
        builder = RestClient.builder();
        mockCatalogo = MockRestServiceServer.bindTo(builder).build();
        servicio = new DeseadosService(jdbc, builder, "http://catalogo.local");
    }

    @Test
    void impideGuardarCursosQueNoEstanPublicados() {
        mockCatalogo.expect(requestTo("http://catalogo.local/api/v1/cursos/2"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess("{\"estado\":\"BORRADOR\"}", org.springframework.http.MediaType.APPLICATION_JSON));
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> servicio.guardar("sub-a", 2L));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verifyNoInteractions(jdbc);
        mockCatalogo.verify();
    }

    @Test
    void noInsertaSiElCursoNoExiste() {
        mockCatalogo.expect(requestTo("http://catalogo.local/api/v1/cursos/998"))
            .andRespond(withResourceNotFound());
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> servicio.guardar("sub-a", 998L));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verifyNoInteractions(jdbc);
        mockCatalogo.verify();
    }

    @Test
    void noAceptaIdentificadoresInvalidos() {
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ResponseStatusException.class,
            () -> servicio.guardar("sub-a", -1L)).getStatusCode());
        assertEquals(HttpStatus.UNAUTHORIZED, assertThrows(ResponseStatusException.class,
            () -> servicio.quitar("", 2L)).getStatusCode());
        verifyNoInteractions(jdbc);
    }

    @Test
    void quitarEsIdempotenteYSiempreFiltraPorElSubject() {
        servicio.quitar("sub-a", 2L);
        servicio.quitar("sub-a", 2L);
        verify(jdbc, times(2)).update(
            eq("DELETE FROM lista_deseados WHERE keycloak_sub = ? AND curso_id = ?"),
            eq("sub-a"), eq(2L));
        mockCatalogo.verify();
    }
}
