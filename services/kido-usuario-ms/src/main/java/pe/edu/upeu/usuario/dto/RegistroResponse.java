package pe.edu.upeu.usuario.dto;

import pe.edu.upeu.usuario.entity.EstadoSolicitud;
import pe.edu.upeu.usuario.entity.TipoCuenta;

/** {@code usuario} es lo que se escribe en el login (el correo). */
public record RegistroResponse(String usuario, TipoCuenta tipo, EstadoSolicitud solicitudDocente, String mensaje) {
}
