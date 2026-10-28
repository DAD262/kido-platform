package pe.edu.upeu.usuario.service;

import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.upeu.usuario.dto.DeseadoResponse;

/**
 * La identidad solo se obtiene del JWT validado, nunca del body ni de la URL.
 * JdbcTemplate usa UPSERT atómico para impedir duplicados en solicitudes concurrentes.
 */
@Service
public class DeseadosService {
    private final JdbcTemplate jdbc;
    private final RestClient catalogo;

    public DeseadosService(JdbcTemplate jdbc, RestClient.Builder builder,
            @Value("${kido.catalogo.base-url:http://kido-curso-1:8080}") String baseUrl) {
        this.jdbc = jdbc;
        this.catalogo = builder.baseUrl(baseUrl).build();
    }

    private static String sujeto(String sub) {
        if (sub == null || sub.isBlank() || sub.length() > 64) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED, "Identidad inválida");
        }
        return sub;
    }

    private static Long cursoId(Long id) {
        if (id == null || id <= 0) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Identificador de curso inválido");
        }
        return id;
    }

    @Transactional(readOnly = true)
    public List<DeseadoResponse> listar(String sub) {
        return jdbc.query("""
                SELECT curso_id, agregado_en FROM lista_deseados
                WHERE keycloak_sub = ? ORDER BY agregado_en DESC, curso_id DESC
                """, (rs, n) -> new DeseadoResponse(rs.getLong(1),
                    rs.getObject(2, OffsetDateTime.class)), sujeto(sub));
    }

    @Transactional
    public DeseadoResponse guardar(String sub, Long id) {
        var usuario = sujeto(sub);
        var curso = cursoId(id);
        // No permitir guardar borradores, cursos archivados ni IDs inventados.
        try {
            CursoPublico actual = catalogo.get().uri("/api/v1/cursos/{id}", curso).retrieve().body(CursoPublico.class);
            if (actual == null || !"PUBLICADO".equals(actual.estado())) {
                throw new ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT,
                        "El curso no está publicado");
            }
        } catch (HttpClientErrorException.NotFound e) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "El curso no existe", e);
        } catch (RestClientException e) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,
                    "No es posible comprobar el catálogo de cursos", e);
        }
        jdbc.update("""
                INSERT INTO lista_deseados (keycloak_sub, curso_id) VALUES (?, ?)
                ON CONFLICT (keycloak_sub, curso_id) DO NOTHING
                """, usuario, curso);
        return jdbc.queryForObject("""
                SELECT curso_id, agregado_en FROM lista_deseados
                WHERE keycloak_sub = ? AND curso_id = ?
                """, (rs, n) -> new DeseadoResponse(rs.getLong(1),
                        rs.getObject(2, OffsetDateTime.class)), usuario, curso);
    }

    @Transactional
    public void quitar(String sub, Long id) {
        jdbc.update("DELETE FROM lista_deseados WHERE keycloak_sub = ? AND curso_id = ?",
                sujeto(sub), cursoId(id));
    }

    public record CursoPublico(String estado) { }
}
