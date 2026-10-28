package pe.edu.upeu.usuario;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Identidad de KIDO. Keycloak guarda las credenciales y los roles; este servicio guarda el perfil
 * académico, el id numérico que usan los demás microservicios y la verificación de docentes.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class KidoUsuarioApplication {
    public static void main(String[] args) {
        SpringApplication.run(KidoUsuarioApplication.class, args);
    }
}
