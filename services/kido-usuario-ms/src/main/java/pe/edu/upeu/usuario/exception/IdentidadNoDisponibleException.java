package pe.edu.upeu.usuario.exception;

/** Keycloak no respondió o rechazó las credenciales del cliente de servicio. */
public class IdentidadNoDisponibleException extends RuntimeException {
    public IdentidadNoDisponibleException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
