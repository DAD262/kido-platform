package pe.edu.upeu.usuario.keycloak;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Cliente confidencial {@code kido-usuario-admin} del realm, con cuenta de servicio y roles
 * realm-management (manage-users, view-users, query-users, view-realm). El secreto llega por .env.
 */
@ConfigurationProperties(prefix = "kido.keycloak")
public record KeycloakAdminProperties(String serverUrl, String realm, String clientId, String clientSecret,
        Duration timeout) {

    public KeycloakAdminProperties {
        if (serverUrl == null || serverUrl.isBlank()) {
            serverUrl = "http://localhost:19090";
        }
        serverUrl = serverUrl.replaceAll("/+$", "");
        if (realm == null || realm.isBlank()) {
            realm = "kido";
        }
        if (clientId == null || clientId.isBlank()) {
            clientId = "kido-usuario-admin";
        }
        if (timeout == null) {
            timeout = Duration.ofSeconds(5);
        }
    }
}
