package pe.edu.upeu.pago.exception;

public class DependenciaNoDisponibleException extends RuntimeException {
    public DependenciaNoDisponibleException(String dependencia, Throwable causa) {
        super("El servicio " + dependencia + " no está disponible temporalmente", causa);
    }
}
