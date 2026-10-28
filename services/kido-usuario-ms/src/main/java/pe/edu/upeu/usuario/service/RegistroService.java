package pe.edu.upeu.usuario.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;
import pe.edu.upeu.usuario.dto.RegistroRequest;
import pe.edu.upeu.usuario.dto.RegistroResponse;
import pe.edu.upeu.usuario.entity.EstadoSolicitud;
import pe.edu.upeu.usuario.entity.Perfil;
import pe.edu.upeu.usuario.entity.SolicitudDocente;
import pe.edu.upeu.usuario.entity.TipoCuenta;
import pe.edu.upeu.usuario.exception.ConflictoException;
import pe.edu.upeu.usuario.keycloak.KeycloakAdminClient;
import pe.edu.upeu.usuario.repository.PerfilRepository;
import pe.edu.upeu.usuario.support.ValidadorRegistro;

/**
 * Registro en dos pasos con compensación:
 * 1) se valida todo (incluido el PDF) sin tocar nada;
 * 2) se crea la cuenta en Keycloak con rol ESTUDIANTE;
 * 3) se guarda perfil (+ solicitud docente) en una transacción; si falla, se borra la cuenta del paso 2.
 */
@Service
public class RegistroService {

    private static final Logger log = LoggerFactory.getLogger(RegistroService.class);
    public static final String ROL_BASE = "ESTUDIANTE";

    private final PerfilRepository perfiles;
    private final SolicitudDocenteService solicitudes;
    private final KeycloakAdminClient keycloak;
    private final TransactionTemplate tx;

    public RegistroService(PerfilRepository perfiles, SolicitudDocenteService solicitudes,
            KeycloakAdminClient keycloak, TransactionTemplate tx) {
        this.perfiles = perfiles;
        this.solicitudes = solicitudes;
        this.keycloak = keycloak;
        this.tx = tx;
    }

    public RegistroResponse registrar(RegistroRequest r, MultipartFile archivo) {
        String email = ValidadorRegistro.email(r.email());
        String dni = ValidadorRegistro.dni(r.dni());
        if(perfiles.existsByDni(dni)) throw new ConflictoException("dni: Ese DNI ya pertenece a una cuenta. Recupera su acceso o consulta a administración.");
        String nombres = ValidadorRegistro.nombre(r.nombres(), "nombres");
        String apellidos = ValidadorRegistro.nombre(r.apellidos(), "apellidos");
        String problema = ValidadorRegistro.problemaContrasena(r.password(), email);
        if (problema != null) {
            throw new IllegalArgumentException("password: " + problema);
        }
        boolean docente = r.tipo() == TipoCuenta.DOCENTE;
        SolicitudDocente.DatosDocente datos = null;
        SolicitudDocenteService.ArchivoPdf pdf = null;
        if (docente) {
            datos = SolicitudDocenteService.normalizar(r.docente());
            if(!dni.equals(datos.dni()))throw new IllegalArgumentException("dni: El DNI profesional debe coincidir con el de tu cuenta.");
            solicitudes.verificarDniLibre(datos.dni(), null);
            pdf = SolicitudDocenteService.leerPdf(archivo, true);
        }
        if (perfiles.existsByEmailIgnoreCase(email) || perfiles.existsByUsernameIgnoreCase(email)) {
            throw new ConflictoException("Ya existe una cuenta con ese correo. Inicia sesión o recupera tu contraseña.");
        }

        String keycloakId = keycloak.crearUsuario(
                new KeycloakAdminClient.NuevoUsuario(email, email, nombres, apellidos, r.password()));
        try {
            keycloak.asignarRolRealm(keycloakId, ROL_BASE);
            SolicitudDocente.DatosDocente datosFinales = datos;
            SolicitudDocenteService.ArchivoPdf pdfFinal = pdf;
            tx.executeWithoutResult(estado -> {
                Perfil nuevo = new Perfil(keycloakId, email, email, nombres, apellidos, r.tipo());
                nuevo.setDni(dni);
                Perfil perfil = perfiles.save(nuevo);
                if (docente) {
                    solicitudes.crearParaPerfil(perfil, datosFinales, pdfFinal);
                }
            });
        } catch (RuntimeException e) {
            log.warn("Registro de {} falló después de crear la cuenta; se revierte en Keycloak", email);
            keycloak.eliminarUsuarioSilencioso(keycloakId);
            throw e;
        }

        log.info("Cuenta {} registrada como {}", email, r.tipo());
        return docente
                ? new RegistroResponse(email, r.tipo(), EstadoSolicitud.PENDIENTE,
                        "Tu cuenta está lista. Ya puedes aprender en KIDO mientras administración verifica tu perfil docente.")
                : new RegistroResponse(email, r.tipo(), null, "Tu cuenta está lista. Inicia sesión para empezar.");
    }

    /** Para validar el correo mientras se escribe. */
    public boolean emailDisponible(String email) {
        String limpio = ValidadorRegistro.email(email);
        if (limpio == null || !limpio.contains("@")) {
            return false;
        }
        return !perfiles.existsByEmailIgnoreCase(limpio) && !keycloak.existeEmail(limpio);
    }
}
