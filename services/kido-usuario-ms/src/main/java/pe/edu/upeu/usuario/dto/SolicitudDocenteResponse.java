package pe.edu.upeu.usuario.dto;

import java.time.LocalDateTime;
import pe.edu.upeu.usuario.entity.DocumentoSolicitud;
import pe.edu.upeu.usuario.entity.EstadoSolicitud;
import pe.edu.upeu.usuario.entity.GradoAcademico;
import pe.edu.upeu.usuario.entity.Perfil;
import pe.edu.upeu.usuario.entity.SolicitudDocente;

public record SolicitudDocenteResponse(
        Long id,
        Long perfilId,
        String nombreCompleto,
        String email,
        String dni,
        String telefono,
        GradoAcademico gradoAcademico,
        String especialidad,
        String institucion,
        int aniosExperiencia,
        String enlaceProfesional,
        String presentacion,
        EstadoSolicitud estado,
        String comentarioRevision,
        String revisadoPor,
        LocalDateTime revisadoEn,
        LocalDateTime enviadoEn,
        int intentos,
        Documento documento) {

    public record Documento(String nombre, long tamano, LocalDateTime subidoEn) {
    }

    public static SolicitudDocenteResponse de(SolicitudDocente s, DocumentoSolicitud doc) {
        Perfil p = s.getPerfil();
        return new SolicitudDocenteResponse(s.getId(), p.getId(), p.nombreCompleto(), p.getEmail(), s.getDni(),
                s.getTelefono(), s.getGradoAcademico(), s.getEspecialidad(), s.getInstitucion(),
                s.getAniosExperiencia(), s.getEnlaceProfesional(), s.getPresentacion(), s.getEstado(),
                s.getComentarioRevision(), s.getRevisadoPor(), s.getRevisadoEn(), s.getEnviadoEn(),
                s.getIntentos(),
                doc == null ? null : new Documento(doc.getNombre(), doc.getTamano(), doc.getSubidoEn()));
    }
}
