package pe.edu.upeu.usuario.dto;

import java.time.LocalDateTime;
import java.util.List;
import pe.edu.upeu.usuario.entity.Perfil;
import pe.edu.upeu.usuario.entity.TipoCuenta;

/**
 * GET /me. {@code id} es el número que el frontend usa como estudianteId y docenteId,
 * así nadie tiene que "vincular" un número a mano.
 */
public record PerfilResponse(
        Long id,
        String username,
        String email,
        String nombres,
        String apellidos,
        TipoCuenta tipoRegistro,
        boolean docenteVerificado,
        List<String> roles,
        LocalDateTime miembroDesde,
        SolicitudDocenteResponse solicitudDocente, String dni) {

    public static PerfilResponse de(Perfil p, List<String> roles, SolicitudDocenteResponse solicitud) {
        return new PerfilResponse(p.getId(), p.getUsername(), p.getEmail(), p.getNombres(), p.getApellidos(),
                p.getTipoRegistro(), p.isDocenteVerificado(), roles, p.getCreadoEn(), solicitud, p.getDni());
    }
}
