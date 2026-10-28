package pe.edu.upeu.usuario.service;
import java.util.*;import java.time.*;import java.nio.charset.StandardCharsets;import java.security.*;
import javax.crypto.Mac;import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;import org.springframework.transaction.support.TransactionTemplate;
import pe.edu.upeu.usuario.keycloak.KeycloakAdminClient;import pe.edu.upeu.usuario.dto.ConfirmarRecuperacionRequest;
import pe.edu.upeu.usuario.repository.PerfilRepository;import pe.edu.upeu.usuario.support.ValidadorRegistro;
@Service
public class RecuperacionService {
 private final JdbcTemplate jdbc;private final TransactionTemplate tx;private final KeycloakAdminClient kc;
 private final CorreoRecuperacion correo;private final PerfilRepository perfiles;private final Clock clock;private final byte[] secret;
 private final SecureRandom random=new SecureRandom();
 public RecuperacionService(JdbcTemplate jdbc,TransactionTemplate tx,KeycloakAdminClient kc,CorreoRecuperacion correo,PerfilRepository perfiles,
 Clock clock,@Value("${kido.recuperacion.secret}")String secret){
  if(secret.length()<32)throw new IllegalArgumentException("Configura KIDO_RECUPERACION_SECRET con al menos 32 caracteres.");
  this.jdbc=jdbc;this.tx=tx;this.kc=kc;this.correo=correo;this.perfiles=perfiles;this.clock=clock;this.secret=secret.getBytes(StandardCharsets.UTF_8);
 }
 public String solicitar(String email){
  String limpio=ValidadorRegistro.email(email);String hash=hash("email:"+limpio);
  return tx.execute(status->{
   var filas=jdbc.queryForList("SELECT id,creado_en,usado FROM recuperaciones_acceso WHERE email_hash=?",hash);
   if(!filas.isEmpty()&&!Boolean.TRUE.equals(filas.get(0).get("usado"))&&((java.sql.Timestamp)filas.get(0).get("creado_en")).toInstant().isAfter(clock.instant().minusSeconds(60)))return filas.get(0).get("id").toString();
   var cuenta=kc.buscarCuentaPorEmail(limpio);String id=UUID.randomUUID().toString();String codigo=String.format(Locale.ROOT,"%06d",random.nextInt(1000000));
   var ahora=java.sql.Timestamp.from(clock.instant());var expira=java.sql.Timestamp.from(clock.instant().plusSeconds(600));
   jdbc.update("INSERT INTO recuperaciones_acceso(id,email_hash,keycloak_id,codigo_hash,creado_en,expira_en) VALUES (?,?,?,?,?,?) ON CONFLICT(email_hash) DO UPDATE SET id=excluded.id,keycloak_id=excluded.keycloak_id,codigo_hash=excluded.codigo_hash,creado_en=excluded.creado_en,expira_en=excluded.expira_en,intentos=0,usado=false",id,hash,cuenta==null?null:cuenta.get("id"),hash(id+":"+codigo),ahora,expira);
   if(cuenta!=null)correo.enviar(limpio,id,codigo);return id;
  });
 }
 public boolean confirmar(ConfirmarRecuperacionRequest r){
  Integer resultado=tx.execute(status->{
   var filas=jdbc.queryForList("SELECT * FROM recuperaciones_acceso WHERE id=? FOR UPDATE",r.solicitudId());if(filas.isEmpty())return -1;
   var fila=filas.get(0);
   if(Boolean.TRUE.equals(fila.get("usado"))||((Number)fila.get("intentos")).intValue()>=5||!((java.sql.Timestamp)fila.get("expira_en")).toInstant().isAfter(clock.instant()))return -1;
   boolean coincide=MessageDigest.isEqual(hash(r.solicitudId()+":"+r.codigo()).getBytes(StandardCharsets.US_ASCII),fila.get("codigo_hash").toString().getBytes(StandardCharsets.US_ASCII));
   if(!coincide||fila.get("keycloak_id")==null){jdbc.update("UPDATE recuperaciones_acceso SET intentos=intentos+1 WHERE id=?",r.solicitudId());return -1;}
   String sub=fila.get("keycloak_id").toString();var cuenta=kc.cuenta(sub);if(!Boolean.TRUE.equals(cuenta.get("enabled")))return -1;
   var roles=kc.rolesCuenta(sub);
   boolean estudiante=roles.contains("ESTUDIANTE")&&roles.stream().noneMatch(x->List.of("ADMIN","DOCENTE","SUPERVISOR").contains(x));
   String clave;
   if(estudiante){
    clave=perfiles.findByKeycloakId(sub).map(p->p.getDni()).orElse(null);
    if(clave==null)throw new IllegalArgumentException("Tu cuenta aún no tiene DNI. Solicita a administración que lo complete antes de recuperar el acceso.");
    clave=ValidadorRegistro.dni(clave);
   }else{
    String problema=ValidadorRegistro.problemaContrasena(r.password(),Objects.toString(cuenta.get("email"),""));
    if(problema!=null)throw new IllegalArgumentException("password: "+problema);
    if(!r.password().equals(r.password().strip()))throw new IllegalArgumentException("password: Evita espacios al inicio o al final.");
    clave=r.password();
   }
   kc.restablecerPassword(sub,clave,estudiante);kc.actualizarCuenta(sub,Map.of("emailVerified",true));
   jdbc.update("UPDATE recuperaciones_acceso SET usado=true WHERE id=?",r.solicitudId());
   org.slf4j.LoggerFactory.getLogger(getClass()).info("Recuperación completada para cuenta {} (DNI temporal={})",sub,estudiante);
   return estudiante?1:0;
  });
  if(resultado==null||resultado<0)throw new IllegalArgumentException("El código no es válido, ha caducado o alcanzó el límite de intentos. Solicita uno nuevo.");return resultado==1;
 }
 String hash(String input){try{Mac mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(secret,"HmacSHA256"));return HexFormat.of().formatHex(mac.doFinal(input.getBytes(StandardCharsets.UTF_8)));}catch(GeneralSecurityException e){throw new IllegalStateException("No se pudo proteger el código de recuperación.");}}
}
