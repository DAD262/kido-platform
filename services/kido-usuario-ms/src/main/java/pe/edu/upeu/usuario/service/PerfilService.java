package pe.edu.upeu.usuario.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import pe.edu.upeu.usuario.dto.PerfilResponse;
import pe.edu.upeu.usuario.dto.SolicitudDocenteResponse;
import pe.edu.upeu.usuario.entity.Perfil;
import pe.edu.upeu.usuario.entity.TipoCuenta;
import pe.edu.upeu.usuario.repository.DocumentoSolicitudRepository;
import pe.edu.upeu.usuario.repository.PerfilRepository;
import pe.edu.upeu.usuario.repository.SolicitudDocenteRepository;
import pe.edu.upeu.usuario.support.ValidadorRegistro;

/** Traduce el JWT de Keycloak al perfil KIDO (y a su id numérico). */
@Service
public class PerfilService {

    private final PerfilRepository perfiles;
    private final SolicitudDocenteRepository solicitudes;
    private final DocumentoSolicitudRepository documentos;
    private final TransactionTemplate tx;
    private final pe.edu.upeu.usuario.keycloak.KeycloakAdminClient keycloak;

    public PerfilService(PerfilRepository perfiles, SolicitudDocenteRepository solicitudes,
            DocumentoSolicitudRepository documentos, TransactionTemplate tx, pe.edu.upeu.usuario.keycloak.KeycloakAdminClient keycloak) {
        this.perfiles = perfiles;
        this.solicitudes = solicitudes;
        this.documentos = documentos;
        this.tx = tx;
        this.keycloak=keycloak;
    }

    /**
     * 1) Perfil ya enlazado al subject del token. 2) Perfil semilla con ese username (cuentas demo).
     * 3) Cuenta creada fuera de KIDO (consola de Keycloak): se crea su perfil al vuelo.
     */
    public Perfil resolver(Jwt jwt) {
        try {
            return tx.execute(estado -> resolverEnTransaccion(jwt));
        } catch (DataIntegrityViolationException carrera) {
            // Dos pestañas pidieron /me a la vez: la otra ya lo creó.
            return perfiles.findByKeycloakId(jwt.getSubject()).orElseThrow(() -> carrera);
        }
    }

    private Perfil resolverEnTransaccion(Jwt jwt) {
        String subject = jwt.getSubject();
        Optional<Perfil> enlazado = perfiles.findByKeycloakId(subject);
        if (enlazado.isPresent()) {
            return enlazado.get();
        }
        String username = Optional.ofNullable(jwt.getClaimAsString("preferred_username")).orElse(subject);
        Optional<Perfil> semilla = perfiles.findByUsernameIgnoreCaseAndKeycloakIdIsNull(username);
        if (semilla.isPresent()) {
            Perfil perfil = semilla.get();
            perfil.setKeycloakId(subject);
            return perfil;
        }
        String email = ValidadorRegistro.email(jwt.getClaimAsString("email"));
        if (email != null && perfiles.existsByEmailIgnoreCase(email)) {
            email = null;
        }
        String nombres = textoONulo(jwt.getClaimAsString("given_name"), username);
        String apellidos = textoONulo(jwt.getClaimAsString("family_name"), "");
        Perfil nuevo = new Perfil(subject, username, email, recortar(nombres), recortar(apellidos), TipoCuenta.ESTUDIANTE);
        if (roles(jwt).contains("DOCENTE")) {
            nuevo.setDocenteVerificado(true);
            nuevo.setTipoRegistro(TipoCuenta.DOCENTE);
        }
        return perfiles.save(nuevo);
    }

    public PerfilResponse me(Jwt jwt) {
        resolver(jwt);
        // El token puede contener datos antiguos. La identidad vigente es la fuente de verdad.
        Map<String,Object> actual=keycloak.cuenta(jwt.getSubject());
        Perfil perfil=tx.execute(status->{
            Perfil p=perfiles.findByKeycloakId(jwt.getSubject()).orElseThrow(()->new pe.edu.upeu.usuario.exception.RecursoNoEncontradoException("El perfil ya no existe."));
            String usuario=java.util.Objects.toString(actual.get("username"),p.getUsername());
            String email=actual.containsKey("email")?ValidadorRegistro.email(java.util.Objects.toString(actual.get("email"),null)):p.getEmail();
            String nombres=recortar(java.util.Objects.toString(actual.get("firstName"),p.getNombres()));
            String apellidos=recortar(java.util.Objects.toString(actual.get("lastName"),p.getApellidos()));
            if(!java.util.Objects.equals(p.getUsername(),usuario)||!java.util.Objects.equals(p.getEmail(),email)||
                !java.util.Objects.equals(p.getNombres(),nombres)||!java.util.Objects.equals(p.getApellidos(),apellidos)) {
                p.setUsername(usuario);p.setEmail(email);p.setNombres(nombres);p.setApellidos(apellidos);perfiles.saveAndFlush(p);
            }
            return p;
        });
        SolicitudDocenteResponse solicitud = solicitudes.findByPerfilId(perfil.getId())
                .map(s -> SolicitudDocenteResponse.de(s, documentos.findBySolicitudId(s.getId()).orElse(null)))
                .orElse(null);
        return PerfilResponse.de(perfil, roles(jwt), solicitud);
    }

    public static List<String> roles(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess != null && realmAccess.get("roles") instanceof Collection<?> roles) {
            return roles.stream().map(String::valueOf)
                    .filter(r -> r.equals("ADMIN") || r.equals("SUPERVISOR") || r.equals("DOCENTE") || r.equals("ESTUDIANTE"))
                    .sorted().toList();
        }
        return List.of();
    }

    private static String textoONulo(String valor, String porDefecto) {
        return valor == null || valor.isBlank() ? porDefecto : valor.strip();
    }

    private static String recortar(String valor) {
        return valor.length() > 80 ? valor.substring(0, 80) : valor;
    }
}
