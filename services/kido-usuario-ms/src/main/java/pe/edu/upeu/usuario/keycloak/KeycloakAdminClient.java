package pe.edu.upeu.usuario.keycloak;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import pe.edu.upeu.usuario.exception.ConflictoException;
import pe.edu.upeu.usuario.exception.IdentidadNoDisponibleException;

/**
 * Llamadas mínimas a la Admin REST API de Keycloak con client_credentials.
 * Solo crea usuarios, les asigna roles de realm y los elimina si el registro falla a medias.
 */
@Component
public class KeycloakAdminClient {

    private static final Logger log = LoggerFactory.getLogger(KeycloakAdminClient.class);
    private static final ParameterizedTypeReference<Map<String, Object>> MAPA = new ParameterizedTypeReference<>() {
    };
    private static final ParameterizedTypeReference<List<Map<String, Object>>> LISTA = new ParameterizedTypeReference<>() {
    };

    private final KeycloakAdminProperties props;
    private final RestClient http;
    private volatile String token;
    private volatile Instant tokenExpira = Instant.EPOCH;

    public KeycloakAdminClient(KeycloakAdminProperties props) {
        this.props = props;
        HttpClient cliente = HttpClient.newBuilder().connectTimeout(props.timeout()).build();
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(cliente);
        fabrica.setReadTimeout(props.timeout());
        this.http = RestClient.builder().baseUrl(props.serverUrl()).requestFactory(fabrica).build();
    }

    public record NuevoUsuario(String username, String email, String nombres, String apellidos, String password) {
        @Override
        public String toString() {
            return "NuevoUsuario[" + username + "]";
        }
    }

    /** Crea el usuario habilitado, con correo verificado y contraseña definitiva. Devuelve su id. */
    public String crearUsuario(NuevoUsuario u) {
        Map<String, Object> cuerpo = Map.of(
                "username", u.username(),
                "email", u.email(),
                "firstName", u.nombres(),
                "lastName", u.apellidos(),
                "enabled", true,
                "emailVerified", true,
                "credentials", List.of(Map.of("type", "password", "value", u.password(), "temporary", false)));
        ResponseEntity<Void> respuesta = conToken(t -> http.post()
                .uri("/admin/realms/{realm}/users", props.realm())
                .headers(h -> h.setBearerAuth(t))
                .contentType(MediaType.APPLICATION_JSON)
                .body(cuerpo)
                .retrieve()
                .toBodilessEntity(), "crear la cuenta");
        URI ubicacion = respuesta.getHeaders().getLocation();
        if (ubicacion == null) {
            String id = buscarIdPorUsername(u.username());
            if (id == null) {
                throw new IdentidadNoDisponibleException("Keycloak no devolvió el id de la cuenta creada.", null);
            }
            return id;
        }
        String ruta = ubicacion.getPath();
        return ruta.substring(ruta.lastIndexOf('/') + 1);
    }

    /** Asigna un rol de realm (ESTUDIANTE, DOCENTE…). Es idempotente. */
    public void asignarRolRealm(String userId, String rol) {
        Map<String, Object> representacion = conToken(t -> http.get()
                .uri("/admin/realms/{realm}/roles/{rol}", props.realm(), rol)
                .headers(h -> h.setBearerAuth(t))
                .retrieve()
                .body(MAPA), "leer el rol " + rol);
        conToken(t -> http.post()
                .uri("/admin/realms/{realm}/users/{id}/role-mappings/realm", props.realm(), userId)
                .headers(h -> h.setBearerAuth(t))
                .contentType(MediaType.APPLICATION_JSON)
                .body(List.of(representacion))
                .retrieve()
                .toBodilessEntity(), "asignar el rol " + rol);
    }

    public String buscarIdPorUsername(String username) {
        List<Map<String, Object>> usuarios = conToken(t -> http.get()
                .uri(b -> b.path("/admin/realms/{realm}/users")
                        .queryParam("username", username)
                        .queryParam("exact", true)
                        .build(props.realm()))
                .headers(h -> h.setBearerAuth(t))
                .retrieve()
                .body(LISTA), "buscar la cuenta");
        return usuarios == null || usuarios.isEmpty() ? null : String.valueOf(usuarios.get(0).get("id"));
    }

    public Map<String,Object> buscarCuentaPorEmail(String email) {
        List<Map<String,Object>> usuarios=conToken(t->http.get()
            .uri(b->b.path("/admin/realms/{realm}/users").queryParam("email",email).queryParam("exact",true).build(props.realm()))
            .headers(h->h.setBearerAuth(t)).retrieve().body(LISTA),"buscar cuenta para recuperación");
        if(usuarios==null)return null;
        return usuarios.stream().filter(u->Boolean.TRUE.equals(u.get("enabled")))
            .filter(u->u.get("serviceAccountClientId")==null&&!String.valueOf(u.get("username")).startsWith("service-account-"))
            .filter(u->email.equalsIgnoreCase(String.valueOf(u.get("email")))).findFirst().orElse(null);
    }
    public String buscarIdPorEmail(String email) {
        List<Map<String,Object>> encontrados=conToken(t->http.get()
            .uri(b->b.path("/admin/realms/{realm}/users").queryParam("email",email).queryParam("exact",true).build(props.realm()))
            .headers(h->h.setBearerAuth(t)).retrieve().body(LISTA),"verificar correo de acceso");
        if(encontrados==null)return null;
        return encontrados.stream().filter(u->email.equalsIgnoreCase(String.valueOf(u.get("email"))))
            .map(u->String.valueOf(u.get("id"))).findFirst().orElse(null);
    }
    public boolean existeEmail(String email) {
        List<Map<String, Object>> usuarios = conToken(t -> http.get()
                .uri(b -> b.path("/admin/realms/{realm}/users")
                        .queryParam("email", email)
                        .queryParam("exact", true)
                        .queryParam("briefRepresentation", true)
                        .build(props.realm()))
                .headers(h -> h.setBearerAuth(t))
                .retrieve()
                .body(LISTA), "verificar el correo");
        return usuarios != null && !usuarios.isEmpty();
    }

    /** Compensación: si el perfil no se pudo guardar, la cuenta no debe quedar huérfana en Keycloak. */
    public void eliminarUsuarioSilencioso(String userId) {
        try {
            conToken(t -> http.delete()
                    .uri("/admin/realms/{realm}/users/{id}", props.realm(), userId)
                    .headers(h -> h.setBearerAuth(t))
                    .retrieve()
                    .toBodilessEntity(), "eliminar la cuenta");
        } catch (RuntimeException e) {
            log.error("No se pudo eliminar la cuenta {} de Keycloak tras un registro fallido; elimínala a mano", userId, e);
        }
    }


    public void quitarRolRealm(String id, String rol) {
        Map<String,Object> r=conToken(t -> http.get().uri("/admin/realms/{realm}/roles/{rol}",props.realm(),rol)
            .headers(h->h.setBearerAuth(t)).retrieve().body(MAPA),"leer rol");
        conToken(t -> http.method(org.springframework.http.HttpMethod.DELETE)
            .uri("/admin/realms/{realm}/users/{id}/role-mappings/realm",props.realm(),id)
            .headers(h->h.setBearerAuth(t)).contentType(MediaType.APPLICATION_JSON).body(List.of(r))
            .retrieve().toBodilessEntity(),"retirar rol supervisor");
    }
    public List<Map<String, Object>> listarCuentas() {
        List<Map<String, Object>> resultado = new java.util.ArrayList<>();
        for (int first = 0; ; first += 100) {
            final int offset = first;
            List<Map<String, Object>> pagina = conToken(t -> http.get()
                .uri(b -> b.path("/admin/realms/{realm}/users").queryParam("first", offset)
                    .queryParam("max", 100).queryParam("briefRepresentation", true).build(props.realm()))
                .headers(h -> h.setBearerAuth(t)).retrieve().body(LISTA), "listar cuentas");
            if (pagina == null || pagina.isEmpty()) break;
            resultado.addAll(pagina);
            if (pagina.size() < 100) break;
        }
        return resultado.stream().filter(u -> !String.valueOf(u.get("username")).startsWith("service-account-"))
            .filter(u -> u.get("serviceAccountClientId") == null).toList();
    }

    public List<String> rolesCuenta(String id) {
        List<Map<String, Object>> roles = conToken(t -> http.get()
            .uri("/admin/realms/{realm}/users/{id}/role-mappings/realm/composite", props.realm(), id)
            .headers(h -> h.setBearerAuth(t)).retrieve().body(LISTA), "leer roles de cuenta");
        return roles == null ? List.of() : roles.stream().map(r -> String.valueOf(r.get("name")))
            .filter(r -> List.of("ADMIN", "SUPERVISOR", "DOCENTE", "ESTUDIANTE").contains(r)).sorted().toList();
    }

    public Map<String, Object> cuenta(String id) {
        return conToken(t -> http.get().uri("/admin/realms/{realm}/users/{id}", props.realm(), id)
            .headers(h -> h.setBearerAuth(t)).retrieve().body(MAPA), "leer cuenta");
    }

    public void actualizarCuenta(String id, Map<String, Object> datos) {
        conToken(t -> http.put().uri("/admin/realms/{realm}/users/{id}", props.realm(), id)
            .headers(h -> h.setBearerAuth(t)).contentType(MediaType.APPLICATION_JSON).body(datos)
            .retrieve().toBodilessEntity(), "actualizar cuenta");
    }

    public void restablecerPassword(String id, String password, boolean temporal) {
        conToken(t -> http.put().uri("/admin/realms/{realm}/users/{id}/reset-password", props.realm(), id)
            .headers(h -> h.setBearerAuth(t)).contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("type", "password", "value", password, "temporary", temporal))
            .retrieve().toBodilessEntity(), "restablecer contraseña");
    }

    private interface Llamada<T> {
        T con(String token);
    }

    /** Ejecuta con un token vigente; si Keycloak responde 401 renueva el token una vez. */
    private <T> T conToken(Llamada<T> llamada, String accion) {
        try {
            try {
                return llamada.con(tokenVigente());
            } catch (HttpClientErrorException.Unauthorized e) {
                invalidarToken();
                return llamada.con(tokenVigente());
            }
        } catch (HttpClientErrorException.Conflict e) {
            throw new ConflictoException("Ya existe una cuenta con ese correo. Inicia sesión o recupera tu contraseña.");
        } catch (HttpClientErrorException.BadRequest e) {
            throw new IllegalArgumentException(traducirError(e));
        } catch (HttpClientErrorException.NotFound e) {
            throw new pe.edu.upeu.usuario.exception.RecursoNoEncontradoException("La cuenta ya no existe.");
        } catch (HttpClientErrorException.Forbidden e) {
            throw new IdentidadNoDisponibleException(
                    "El cliente kido-usuario-admin no tiene permisos en Keycloak para " + accion
                            + ". Revisa los roles de su cuenta de servicio (GUIA_LEVANTAR_KIDO.md).", e);
        } catch (RestClientResponseException e) {
            throw new IdentidadNoDisponibleException(
                    "Keycloak respondió " + e.getStatusCode().value() + " al " + accion + ".", e);
        } catch (ResourceAccessException e) {
            throw new IdentidadNoDisponibleException(
                    "No se pudo contactar a Keycloak para " + accion + ". Intenta de nuevo en unos segundos.", e);
        }
    }

    private String traducirError(RestClientResponseException e) {
        String detalle = e.getResponseBodyAsString();
        if (detalle.contains("invalidPassword") || detalle.toLowerCase().contains("password")) {
            return "password: la contraseña no cumple la política de seguridad (8+ caracteres, mayúscula, minúscula y número).";
        }
        if (detalle.toLowerCase().contains("email")) {
            return "email: Keycloak rechazó el correo. Revisa que esté bien escrito.";
        }
        return "Keycloak rechazó los datos de la cuenta.";
    }

    private void invalidarToken() {
        token = null;
        tokenExpira = Instant.EPOCH;
    }

    private synchronized String tokenVigente() {
        if (token != null && Instant.now().isBefore(tokenExpira)) {
            return token;
        }
        if (props.clientSecret() == null || props.clientSecret().isBlank()) {
            throw new IdentidadNoDisponibleException(
                    "Falta KIDO_USUARIO_ADMIN_SECRET: el servicio de usuarios no puede crear cuentas en Keycloak.", null);
        }
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", props.clientId());
        form.add("client_secret", props.clientSecret());
        Map<String, Object> respuesta;
        try {
            respuesta = http.post()
                    .uri("/realms/{realm}/protocol/openid-connect/token", props.realm())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                    .body(form)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> {
                        throw new IdentidadNoDisponibleException(
                                "Keycloak rechazó al cliente " + props.clientId() + " (HTTP " + res.getStatusCode().value()
                                        + "). Revisa KIDO_USUARIO_ADMIN_SECRET y que el realm se haya importado.", null);
                    })
                    .body(MAPA);
        } catch (ResourceAccessException e) {
            throw new IdentidadNoDisponibleException(
                    "No se pudo contactar a Keycloak en " + props.serverUrl() + ".", e);
        }
        if (respuesta == null || respuesta.get("access_token") == null) {
            throw new IdentidadNoDisponibleException("Keycloak no entregó un token de servicio.", null);
        }
        token = String.valueOf(respuesta.get("access_token"));
        long segundos = respuesta.get("expires_in") instanceof Number n ? n.longValue() : 60;
        tokenExpira = Instant.now().plusSeconds(Math.max(10, segundos - 15));
        return token;
    }

}
