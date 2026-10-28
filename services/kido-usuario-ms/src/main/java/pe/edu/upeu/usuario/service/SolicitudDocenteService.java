package pe.edu.upeu.usuario.service;

import java.io.IOException;
import java.util.EnumSet;
import java.util.List;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;
import pe.edu.upeu.usuario.dto.DatosDocenteRequest;
import pe.edu.upeu.usuario.dto.ResumenSolicitudes;
import pe.edu.upeu.usuario.dto.SolicitudDocenteResponse;
import pe.edu.upeu.usuario.entity.DocumentoSolicitud;
import pe.edu.upeu.usuario.entity.EstadoSolicitud;
import pe.edu.upeu.usuario.entity.Perfil;
import pe.edu.upeu.usuario.entity.SolicitudDocente;
import pe.edu.upeu.usuario.entity.TipoCuenta;
import pe.edu.upeu.usuario.exception.ConflictoException;
import pe.edu.upeu.usuario.exception.RecursoNoEncontradoException;
import pe.edu.upeu.usuario.keycloak.KeycloakAdminClient;
import pe.edu.upeu.usuario.repository.DocumentoSolicitudRepository;
import pe.edu.upeu.usuario.repository.PerfilRepository;
import pe.edu.upeu.usuario.repository.SolicitudDocenteRepository;
import pe.edu.upeu.usuario.support.ValidadorRegistro;

/**
 * Verificación de docentes. Nadie obtiene el rol DOCENTE por registrarse: queda como
 * ESTUDIANTE con una solicitud PENDIENTE y administración la aprueba, observa o rechaza.
 */
@Service
public class SolicitudDocenteService {

    public static final String ROL_DOCENTE = "DOCENTE";

    private final SolicitudDocenteRepository solicitudes;
    private final DocumentoSolicitudRepository documentos;
    private final PerfilRepository perfiles;
    private final PerfilService perfilService;
    private final KeycloakAdminClient keycloak;
    private final TransactionTemplate tx;

    public SolicitudDocenteService(SolicitudDocenteRepository solicitudes, DocumentoSolicitudRepository documentos,
            PerfilRepository perfiles, PerfilService perfilService, KeycloakAdminClient keycloak,
            TransactionTemplate tx) {
        this.solicitudes = solicitudes;
        this.documentos = documentos;
        this.perfiles = perfiles;
        this.perfilService = perfilService;
        this.keycloak = keycloak;
        this.tx = tx;
    }

    /** Documento ya validado y leído en memoria. */
    public record ArchivoPdf(String nombre, byte[] contenido) {
    }

    public static ArchivoPdf leerPdf(MultipartFile archivo, boolean obligatorio) {
        if (archivo == null || archivo.isEmpty()) {
            if (obligatorio) {
                throw new IllegalArgumentException("documento: adjunta tu CV, título o constancia en PDF.");
            }
            return null;
        }
        try {
            byte[] contenido = archivo.getBytes();
            ValidadorRegistro.documentoPdf(archivo.getOriginalFilename(), contenido.length, contenido);
            return new ArchivoPdf(ValidadorRegistro.nombreArchivo(archivo.getOriginalFilename()), contenido);
        } catch (IOException e) {
            throw new IllegalArgumentException("documento: no se pudo leer el archivo. Vuelve a adjuntarlo.");
        }
    }

    /** Valida y normaliza el formulario de docente. */
    public static SolicitudDocente.DatosDocente normalizar(DatosDocenteRequest d) {
        if (d == null) {
            throw new IllegalArgumentException("Completa los datos de docente para verificar tu perfil.");
        }
        return new SolicitudDocente.DatosDocente(
                ValidadorRegistro.dni(d.dni()),
                ValidadorRegistro.celular(d.telefono()),
                d.gradoAcademico(),
                ValidadorRegistro.limpiar(d.especialidad()),
                ValidadorRegistro.limpiar(d.institucion()),
                d.aniosExperiencia(),
                ValidadorRegistro.enlace(d.enlaceProfesional()),
                d.presentacion().strip(),
                d.declaracionAceptada());
    }

    public void verificarDniLibre(String dni, Long perfilId) {
        boolean usado = perfilId == null
                ? solicitudes.existsByDniAndEstadoNot(dni, EstadoSolicitud.RECHAZADA)
                : solicitudes.existsByDniAndEstadoNotAndPerfilIdNot(dni, EstadoSolicitud.RECHAZADA, perfilId);
        if (usado) {
            throw new ConflictoException("Ese DNI ya figura en otra solicitud docente. Si es tuyo, escribe a administración.");
        }
    }

    /** Usado por el registro, dentro de su transacción. */
    void crearParaPerfil(Perfil perfil, SolicitudDocente.DatosDocente datos, ArchivoPdf pdf) {
        SolicitudDocente solicitud = new SolicitudDocente(perfil);
        solicitud.enviar(datos);
        solicitud = solicitudes.save(solicitud);
        DocumentoSolicitud doc = new DocumentoSolicitud(solicitud.getId());
        doc.reemplazar(pdf.nombre(), "application/pdf", pdf.contenido());
        documentos.save(doc);
    }

    /** Un estudiante que ya tiene cuenta postula (o corrige una solicitud observada). */
    public SolicitudDocenteResponse enviarPropia(Jwt jwt, DatosDocenteRequest request, MultipartFile archivo) {
        Perfil perfil = perfilService.resolver(jwt);
        if (PerfilService.roles(jwt).contains(ROL_DOCENTE) || perfil.isDocenteVerificado()) {
            throw new ConflictoException("Tu cuenta ya es docente verificada.");
        }
        SolicitudDocente.DatosDocente datos = normalizar(request);
        if(perfil.getDni()!=null && !perfil.getDni().equals(datos.dni()))throw new IllegalArgumentException("dni: Debe coincidir con el DNI registrado en tu cuenta.");
        verificarDniLibre(datos.dni(), perfil.getId());
        SolicitudDocente existente = solicitudes.findByPerfilId(perfil.getId()).orElse(null);
        if (existente != null && existente.getEstado() == EstadoSolicitud.PENDIENTE) {
            throw new ConflictoException("Tu solicitud ya está en revisión. Te avisaremos cuando administración responda.");
        }
        boolean tieneDocumento = existente != null && documentos.findBySolicitudId(existente.getId()).isPresent();
        ArchivoPdf pdf = leerPdf(archivo, !tieneDocumento);
        return tx.execute(estado -> {
            Perfil p = perfiles.findById(perfil.getId()).orElseThrow();
            p.setTipoRegistro(TipoCuenta.DOCENTE);
            SolicitudDocente s = existente == null ? new SolicitudDocente(p)
                    : solicitudes.findById(existente.getId()).orElseThrow();
            s.enviar(datos);
            s = solicitudes.save(s);
            DocumentoSolicitud doc = documentos.findBySolicitudId(s.getId()).orElse(null);
            if (pdf != null) {
                if (doc == null) {
                    doc = new DocumentoSolicitud(s.getId());
                }
                doc.reemplazar(pdf.nombre(), "application/pdf", pdf.contenido());
                doc = documentos.save(doc);
            }
            return SolicitudDocenteResponse.de(s, doc);
        });
    }

    public DocumentoSolicitud documentoPropio(Jwt jwt) {
        Perfil perfil = perfilService.resolver(jwt);
        SolicitudDocente s = solicitudes.findByPerfilId(perfil.getId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Aún no enviaste una solicitud docente."));
        return documento(s.getId());
    }

    // ---- Administración ----

    @Transactional(readOnly = true)
    public List<SolicitudDocenteResponse> listar(String filtro) {
        List<SolicitudDocente> lista;
        if (filtro == null || filtro.isBlank() || filtro.equalsIgnoreCase("TODAS")) {
            lista = solicitudes.findAllByOrderByEnviadoEnDesc();
        } else if (filtro.equalsIgnoreCase("ACTIVAS")) {
            lista = solicitudes.findByEstadoInOrderByEnviadoEnAsc(EnumSet.of(EstadoSolicitud.PENDIENTE, EstadoSolicitud.OBSERVADA));
        } else {
            EstadoSolicitud estado;
            try {
                estado = EstadoSolicitud.valueOf(filtro.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Filtro de estado desconocido: " + filtro);
            }
            lista = solicitudes.findByEstadoInOrderByEnviadoEnAsc(EnumSet.of(estado));
        }
        return lista.stream()
                .map(s -> SolicitudDocenteResponse.de(s, documentos.findBySolicitudId(s.getId()).orElse(null)))
                .toList();
    }

    @Transactional(readOnly = true)
    public ResumenSolicitudes resumen() {
        return new ResumenSolicitudes(
                solicitudes.countByEstado(EstadoSolicitud.PENDIENTE),
                solicitudes.countByEstado(EstadoSolicitud.OBSERVADA),
                solicitudes.countByEstado(EstadoSolicitud.APROBADA),
                solicitudes.countByEstado(EstadoSolicitud.RECHAZADA));
    }

    @Transactional(readOnly = true)
    public SolicitudDocenteResponse obtener(Long id) {
        SolicitudDocente s = buscar(id);
        return SolicitudDocenteResponse.de(s, documentos.findBySolicitudId(id).orElse(null));
    }

    @Transactional(readOnly = true)
    public DocumentoSolicitud documento(Long solicitudId) {
        return documentos.findBySolicitudId(solicitudId)
                .orElseThrow(() -> new RecursoNoEncontradoException("La solicitud no tiene documento adjunto."));
    }

    /**
     * Primero Keycloak (rol DOCENTE), luego la base. Si Keycloak falla no se marca como aprobada;
     * si la base falla después, reintentar es seguro porque asignar un rol es idempotente.
     */
    public SolicitudDocenteResponse aprobar(Long id, String comentario, String revisor) {
        SolicitudDocente s = buscar(id);
        exigirRevisable(s);
        Perfil perfil = s.getPerfil();
        String keycloakId = perfil.getKeycloakId();
        if (keycloakId == null) {
            keycloakId = keycloak.buscarIdPorUsername(perfil.getUsername());
            if (keycloakId == null) {
                throw new RecursoNoEncontradoException("La cuenta " + perfil.getUsername() + " no existe en Keycloak.");
            }
        }
        keycloak.asignarRolRealm(keycloakId, ROL_DOCENTE);
        String idKeycloak = keycloakId;
        return tx.execute(estado -> {
            SolicitudDocente actual = buscar(id);
            Perfil p = actual.getPerfil();
            if (p.getKeycloakId() == null) {
                p.setKeycloakId(idKeycloak);
            }
            p.setDocenteVerificado(true);
            perfiles.save(p);
            actual.revisar(EstadoSolicitud.APROBADA, textoOpcional(comentario), revisor);
            return SolicitudDocenteResponse.de(solicitudes.save(actual), documentos.findBySolicitudId(id).orElse(null));
        });
    }

    public SolicitudDocenteResponse observar(Long id, String comentario, String revisor) {
        return cerrarSinRol(id, EstadoSolicitud.OBSERVADA, comentario, revisor);
    }

    public SolicitudDocenteResponse rechazar(Long id, String comentario, String revisor) {
        return cerrarSinRol(id, EstadoSolicitud.RECHAZADA, comentario, revisor);
    }

    private SolicitudDocenteResponse cerrarSinRol(Long id, EstadoSolicitud estado, String comentario, String revisor) {
        String motivo = textoOpcional(comentario);
        if (motivo == null || motivo.length() < 10) {
            throw new IllegalArgumentException("comentario: explica en al menos 10 caracteres qué debe corregir o por qué se rechaza.");
        }
        return tx.execute(t -> {
            SolicitudDocente s = buscar(id);
            exigirRevisable(s);
            s.revisar(estado, motivo, revisor);
            return SolicitudDocenteResponse.de(solicitudes.save(s), documentos.findBySolicitudId(id).orElse(null));
        });
    }

    private static void exigirRevisable(SolicitudDocente s) {
        if (s.getEstado() != EstadoSolicitud.PENDIENTE && s.getEstado() != EstadoSolicitud.OBSERVADA) {
            throw new ConflictoException("Esta solicitud ya fue " + s.getEstado().name().toLowerCase() + ".");
        }
    }

    private SolicitudDocente buscar(Long id) {
        return solicitudes.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la solicitud docente " + id + "."));
    }

    private static String textoOpcional(String valor) {
        return valor == null || valor.isBlank() ? null : valor.strip();
    }
}
