package pe.edu.upeu.usuario.service;

import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import pe.edu.upeu.usuario.exception.*;

/** El docente se resuelve desde la sesión. Solo se devuelven nombres vinculados a sus cursos. */
@Service
public class DocenteSeguimientoService {
 private static final ParameterizedTypeReference<List<Map<String,Object>>> LIST=new ParameterizedTypeReference<>(){};
 private final JdbcTemplate jdbc;private final RestClient cursos,inscripciones,pagos;
 public DocenteSeguimientoService(JdbcTemplate jdbc,
  @Value("${kido.catalogo.base-url:http://kido-curso-1:8080}") String catalogo,
  @Value("${kido.supervision.inscripciones-url:http://kido-inscripcion-ms:8080}") String inscripcionesUrl,
  @Value("${kido.docente.pagos-url:http://kido-pago-ms:8080}") String pagosUrl){
  this.jdbc=jdbc;var factory=new org.springframework.http.client.JdkClientHttpRequestFactory(java.net.http.HttpClient.newBuilder().connectTimeout(java.time.Duration.ofSeconds(3)).build());factory.setReadTimeout(java.time.Duration.ofSeconds(5));
  cursos=RestClient.builder().baseUrl(catalogo).requestFactory(factory).build();inscripciones=RestClient.builder().baseUrl(inscripcionesUrl).requestFactory(factory).build();pagos=RestClient.builder().baseUrl(pagosUrl).requestFactory(factory).build();
 }
 public record Seguimiento(List<Map<String,Object>> inscripciones,List<Map<String,Object>> compras,List<String> fallos){}
 public Seguimiento consultar(String sub,String bearer){
  var perfiles=jdbc.queryForList("SELECT id FROM perfiles WHERE keycloak_id=?",Long.class,sub);
  if(perfiles.isEmpty())throw new RecursoNoEncontradoException("No se encontró el perfil del docente.");Long docente=perfiles.getFirst();
  List<Map<String,Object>> propios;
  try{propios=cursos.get().uri("/internal/v1/cursos/docente/{id}",docente).retrieve().body(LIST);}
  catch(RestClientException e){throw new IdentidadNoDisponibleException("No se pudieron consultar los cursos del docente.",e);}
  List<Map<String,Object>> matriculas=new ArrayList<>(),compras=new ArrayList<>();List<String> fallos=new ArrayList<>();
  for(var c:propios==null?List.<Map<String,Object>>of():propios){
   if(!(c.get("docenteId") instanceof Number d)||d.longValue()!=docente)continue;
   var cursoId=((Number)c.get("id")).longValue();
   try{var lista=inscripciones.get().uri("/internal/v1/inscripciones/curso/{id}",cursoId).retrieve().body(LIST);
    for(var i:lista==null?List.<Map<String,Object>>of():lista){if(!(i.get("cursoId") instanceof Number n)||n.longValue()!=cursoId)continue;var r=new LinkedHashMap<>(i);r.put("cursoTitulo",c.get("titulo"));matriculas.add(r);}
   }catch(RestClientException e){fallos.add("No se pudieron cargar los estudiantes de "+c.get("titulo")+". Reintenta.");}
  }
  try{var lista=pagos.get().uri("/api/v1/pagos/docentes/{id}/historial",docente).header("Authorization",bearer).retrieve().body(LIST);
   for(var o:lista==null?List.<Map<String,Object>>of():lista){if(o.get("docenteId") instanceof Number n&&n.longValue()==docente)compras.add(new LinkedHashMap<>(o));}
  }catch(RestClientException e){fallos.add("No se pudo cargar el historial de compras. Reintenta.");}
  Set<Long> ids=new LinkedHashSet<>();for(var lista:List.of(matriculas,compras))for(var r:lista)if(r.get("estudianteId") instanceof Number n)ids.add(n.longValue());
  Map<Long,String> nombres=new HashMap<>();
  if(!ids.isEmpty())jdbc.query("SELECT id,TRIM(CONCAT_WS(' ',nombres,apellidos)) AS nombre FROM perfiles WHERE id IN ("+String.join(",",Collections.nCopies(ids.size(),"?"))+")",rs->{String nombre=rs.getString("nombre");if(nombre!=null&&!nombre.isBlank())nombres.put(rs.getLong("id"),nombre);},ids.toArray());
  for(var lista:List.of(matriculas,compras))for(var r:lista)if(r.get("estudianteId") instanceof Number n)r.put("estudianteNombre",nombres.getOrDefault(n.longValue(),"Estudiante #"+n.longValue()));
  return new Seguimiento(matriculas,compras,fallos);
 }
}
