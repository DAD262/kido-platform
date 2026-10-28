package pe.edu.upeu.usuario.service;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import pe.edu.upeu.usuario.dto.*;
import pe.edu.upeu.usuario.keycloak.KeycloakAdminClient;
import pe.edu.upeu.usuario.repository.PerfilRepository;
import pe.edu.upeu.usuario.exception.ConflictoException;
import pe.edu.upeu.usuario.support.ValidadorRegistro;

@Service
public class AdminCuentaService {
    private static final Logger log = LoggerFactory.getLogger(AdminCuentaService.class);
    private final KeycloakAdminClient keycloak;
    private final PerfilRepository perfiles;
    private final TransactionTemplate tx;
    public AdminCuentaService(KeycloakAdminClient keycloak, PerfilRepository perfiles, TransactionTemplate tx) {
        this.keycloak=keycloak; this.perfiles=perfiles; this.tx=tx;
    }
    public List<CuentaAdminResponse> listar() {
        List<Map<String,Object>> identidades=keycloak.listarCuentas();
        Set<String> ids=new HashSet<>(); Set<String> usuarios=new HashSet<>();
        List<CuentaAdminResponse> resultado=new ArrayList<>();
        for(var u:identidades){ids.add(texto(u,"id"));usuarios.add(texto(u,"username").toLowerCase(Locale.ROOT));
            resultado.add(respuesta(u,keycloak.rolesCuenta(texto(u,"id"))));}
        for(var p:perfiles.findAll()) {
            if(!ids.contains(p.getKeycloakId()) && !usuarios.contains(p.getUsername().toLowerCase(Locale.ROOT)))
                resultado.add(new CuentaAdminResponse("historico-"+p.getId(),p.getUsername(),p.getEmail()==null?"":p.getEmail(),
                    p.getNombres(),p.getApellidos(),false,List.of(),true,p.getId(),p.getDni()));
        }
        return resultado.stream().sorted(Comparator.comparing(CuentaAdminResponse::username,String.CASE_INSENSITIVE_ORDER)).toList();
    }
    private CuentaAdminResponse respuesta(Map<String,Object> u, List<String> roles) {
        return new CuentaAdminResponse(texto(u,"id"), texto(u,"username"), texto(u,"email"),
            texto(u,"firstName"), texto(u,"lastName"), Boolean.TRUE.equals(u.get("enabled")), roles, false, null, perfiles.findByKeycloakId(texto(u,"id")).map(p -> p.getDni()).orElse(null));
    }
    private Map<String,Object> persona(String id) {
        Map<String,Object> u = keycloak.cuenta(id);
        if (u == null || u.get("serviceAccountClientId") != null || texto(u,"username").startsWith("service-account-"))
            throw new IllegalArgumentException("Las cuentas técnicas no se administran desde este panel.");
        return u;
    }
    public CuentaAdminResponse actualizar(String actor, String id, ActualizarCuentaRequest r) {
        Map<String,Object> anterior = persona(id);
        List<String> roles = keycloak.rolesCuenta(id);
        if (!r.habilitada()) {
            if (actor.equals(id)) throw new IllegalArgumentException("No puedes desactivar tu propia cuenta.");
            // Los administradores se mantienen activos para impedir perder el acceso administrativo.
            if (roles.contains("ADMIN")) throw new IllegalArgumentException("No se puede desactivar una cuenta administradora desde este panel.");
        }
        String nombres=ValidadorRegistro.nombre(r.nombres(),"nombres");
        String apellidos=ValidadorRegistro.nombre(r.apellidos(),"apellidos");
        String email=ValidadorRegistro.email(r.email());
        String username=r.username()==null?texto(anterior,"username"):r.username().strip().toLowerCase(Locale.ROOT);
        if(!username.matches("[a-z0-9][a-z0-9._@+-]{2,159}"))throw new IllegalArgumentException("username: Usa de 3 a 160 letras, números o los símbolos . _ @ + - sin espacios.");
        if(username.startsWith("service-account-"))throw new IllegalArgumentException("username: Ese prefijo está reservado para cuentas técnicas.");
        String mismoUsuario=keycloak.buscarIdPorUsername(username);
        if(mismoUsuario!=null&&!id.equals(mismoUsuario))throw new ConflictoException("username: Ese usuario de acceso ya pertenece a otra cuenta.");
        String correoComoUsuario=keycloak.buscarIdPorUsername(email);
        if(correoComoUsuario!=null&&!id.equals(correoComoUsuario))throw new ConflictoException("email: Ese correo se usa como usuario de acceso de otra persona.");
        if(username.contains("@")){
            String usuarioComoCorreo=keycloak.buscarIdPorEmail(username);
            if(usuarioComoCorreo!=null&&!id.equals(usuarioComoCorreo))throw new ConflictoException("username: Ese usuario coincide con el correo de otra persona.");
        }
        var perfilUsuario=perfiles.findByUsernameIgnoreCase(username).filter(p->!id.equals(p.getKeycloakId()));
        if(perfilUsuario.isPresent())throw new ConflictoException("username: Ese usuario está reservado por otro perfil. Elige otro nombre.");
        var reservado=perfiles.findByEmailIgnoreCase(email).filter(p -> !id.equals(p.getKeycloakId()));
        Long historicoId=null;
        if(reservado.isPresent()) {
            var p=reservado.get();
            // Solo un 404 confirmado permite liberar un correo: nunca ante errores de red o permisos.
            if(p.getKeycloakId()==null)throw new ConflictoException("El correo pertenece a un perfil pendiente de vinculación. Revisa sus datos antes de reutilizarlo.");
            try {
                keycloak.cuenta(p.getKeycloakId());
                throw new ConflictoException("Ese correo ya está vinculado a otra cuenta activa.");
            } catch(pe.edu.upeu.usuario.exception.RecursoNoEncontradoException ausente) { historicoId=p.getId(); }
        }
        final Long liberarId=historicoId;
        String dni=r.dni()==null||r.dni().isBlank()?null:ValidadorRegistro.dni(r.dni());
        if(dni!=null && perfiles.existsByDniAndKeycloakIdNot(dni,id))throw new ConflictoException("Ese DNI ya pertenece a otra cuenta.");
        Map<String,Object> cambios = new HashMap<>();
        cambios.put("firstName",nombres); cambios.put("lastName",apellidos);
        cambios.put("username",username);
        cambios.put("email",email); cambios.put("enabled",r.habilitada());
        cambios.put("emailVerified", email.equalsIgnoreCase(texto(anterior,"email")) && Boolean.TRUE.equals(anterior.get("emailVerified")));
        keycloak.actualizarCuenta(id,cambios);
        try {
            tx.executeWithoutResult(status -> {
                if(liberarId!=null)perfiles.findById(liberarId).ifPresent(historico->{historico.setEmail(null);perfiles.saveAndFlush(historico);});
                perfiles.findByKeycloakId(id).ifPresent(p -> {
                    if(dni!=null)p.setDni(dni); p.setUsername(username); p.setEmail(email); p.setNombres(nombres); p.setApellidos(apellidos); perfiles.saveAndFlush(p);
                });
            });
            if(liberarId!=null)log.info("ADMIN {} liberó correo del perfil histórico {} al editar cuenta {}",actor,liberarId,id);
        } catch (RuntimeException e) {
            Map<String,Object> restaurar = new HashMap<>();
            for(String campo: List.of("username","firstName","lastName","email","enabled","emailVerified"))
                if(anterior.containsKey(campo)) restaurar.put(campo,anterior.get(campo));
            try { keycloak.actualizarCuenta(id,restaurar); }
            catch(RuntimeException fallo) { log.error("No se pudo compensar edición de cuenta {} por {}",id,actor); e.addSuppressed(fallo); }
            throw e;
        }
        log.info("ADMIN {} actualizó cuenta {}",actor,id);
        return respuesta(keycloak.cuenta(id),roles);
    }
    public void restablecer(String actor,String id,RestablecerPasswordRequest r) {
        Map<String,Object> u=persona(id);
        String error=ValidadorRegistro.problemaContrasena(r.password(),texto(u,"email"));
        if(error!=null) throw new IllegalArgumentException("password: "+error);
        keycloak.restablecerPassword(id,r.password(),r.temporal());
        log.info("ADMIN {} restableció contraseña de cuenta {} (temporal={})",actor,id,r.temporal());
    }
    private static String texto(Map<String,Object> u,String campo) { return Objects.toString(u.get(campo),""); }
}
