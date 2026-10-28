package pe.edu.upeu.usuario.service;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.ResourceAccessException;
import pe.edu.upeu.usuario.dto.SupervisionRequest;
import pe.edu.upeu.usuario.keycloak.KeycloakAdminClient;
import pe.edu.upeu.usuario.exception.*;
@Service
public class SupervisionService {
    private static final ParameterizedTypeReference<Map<String,Object>> MAP=new ParameterizedTypeReference<>(){};
    private static final ParameterizedTypeReference<List<Map<String,Object>>> LIST=new ParameterizedTypeReference<>(){};
    private final JdbcTemplate jdbc; private final TransactionTemplate tx; private final KeycloakAdminClient kc;
    private final RestClient cursos; private final RestClient inscripciones;
    public SupervisionService(JdbcTemplate jdbc,TransactionTemplate tx,KeycloakAdminClient kc,
        @Value("${kido.catalogo.base-url:http://kido-curso-1:8080}") String catalogo,
        @Value("${kido.supervision.inscripciones-url:http://kido-inscripcion-ms:8080}") String inscripcionesUrl) {
        this.jdbc=jdbc;this.tx=tx;this.kc=kc;
        var factory=new org.springframework.http.client.JdkClientHttpRequestFactory(
            java.net.http.HttpClient.newBuilder().connectTimeout(java.time.Duration.ofSeconds(5)).build());
        factory.setReadTimeout(java.time.Duration.ofSeconds(10));
        cursos=RestClient.builder().baseUrl(catalogo).requestFactory(factory).build();
        inscripciones=RestClient.builder().baseUrl(inscripcionesUrl).requestFactory(factory).build();
    }
    public List<Long> ids(String sub) { return jdbc.queryForList("SELECT curso_id FROM supervision_cursos WHERE supervisor_sub=? ORDER BY curso_id",Long.class,sub); }
    public void asignar(String actor,String sub,SupervisionRequest r) {
        Map<String,Object> u=kc.cuenta(sub);
        if(u.get("serviceAccountClientId")!=null || Objects.toString(u.get("username"),"").startsWith("service-account-"))
            throw new IllegalArgumentException("No se asignan clases a cuentas técnicas.");
        List<Long> nuevos=r.habilitada()?r.cursos().stream().distinct().toList():List.of();
        for(Long id:nuevos) curso(id);
        boolean yaSupervisor=kc.rolesCuenta(sub).contains("SUPERVISOR");
        if(r.habilitada())kc.asignarRolRealm(sub,"SUPERVISOR");else kc.quitarRolRealm(sub,"SUPERVISOR");
        try { tx.executeWithoutResult(status->{
            jdbc.update("DELETE FROM supervision_cursos WHERE supervisor_sub=?",sub);
            for(Long id:nuevos)jdbc.update("INSERT INTO supervision_cursos(supervisor_sub,curso_id,asignado_por) VALUES (?,?,?)",sub,id,actor);
        }); } catch(RuntimeException e) {
            if(yaSupervisor)kc.asignarRolRealm(sub,"SUPERVISOR");else kc.quitarRolRealm(sub,"SUPERVISOR");
            throw e;
        }
        org.slf4j.LoggerFactory.getLogger(getClass()).info("ADMIN {} actualizó supervisión de {}: {} cursos",actor,sub,nuevos.size());
    }
    public List<Map<String,Object>> listar(String sub) {
        return ids(sub).stream().map(id->{Map<String,Object> c=curso(id);return resumen(c);}).toList();
    }
    public Map<String,Object> seguimiento(String sub,Long id) {
        if(!ids(sub).contains(id))throw new RecursoNoEncontradoException("El curso no está asignado a tu supervisión.");
        Map<String,Object> curso=curso(id);
        try {
            List<Map<String,Object>> inscritos=inscripciones.get().uri("/internal/v1/inscripciones/curso/{id}",id).retrieve().body(LIST);
            List<Map<String,Object>> estudiantes=(inscritos==null?List.<Map<String,Object>>of():inscritos).stream().map(i->{
                Map<String,Object> alumno=new LinkedHashMap<>();alumno.put("id",i.get("estudianteId"));
                alumno.put("nombre",nombre(((Number)i.get("estudianteId")).longValue()));
                alumno.put("estado",i.get("estado"));alumno.put("progreso",i.get("porcentajeProgreso"));return alumno;
            }).toList();
            Map<String,Object> contenido=cursos.get().uri("/api/v1/cursos/{id}/contenido",id).retrieve().body(MAP);
            Map<String,Object> resultado=new LinkedHashMap<>();resultado.put("curso",resumen(curso));
            resultado.put("estudiantes",estudiantes);resultado.put("contenido",contenido);return resultado;
        }catch(RestClientResponseException|ResourceAccessException e){throw new IdentidadNoDisponibleException("El seguimiento no está disponible temporalmente. Reintenta.",e);}
    }
    private Map<String,Object> resumen(Map<String,Object> c) {
        Map<String,Object> r=new LinkedHashMap<>();
        for(String key:List.of("id","titulo","docenteId","estado"))r.put(key,c.get(key));
        r.put("docente",nombre(((Number)c.get("docenteId")).longValue()));return r;
    }
    private String nombre(Long id) {
        List<String> nombres=jdbc.queryForList("SELECT nombres || ' ' || apellidos FROM perfiles WHERE id=?",String.class,id);
        return nombres.isEmpty()?"Usuario #"+id:nombres.get(0);
    }
    private Map<String,Object> curso(Long id) {
        try {return cursos.get().uri("/api/v1/cursos/{id}",id).retrieve().body(MAP);}
        catch(org.springframework.web.client.HttpClientErrorException.NotFound e){throw new RecursoNoEncontradoException("El curso #"+id+" no existe.");}
        catch(RestClientResponseException|ResourceAccessException e){throw new IdentidadNoDisponibleException("El catálogo no está disponible temporalmente.",e);}
    }
}
