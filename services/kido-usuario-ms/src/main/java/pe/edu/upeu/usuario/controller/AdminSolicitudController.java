package pe.edu.upeu.usuario.controller;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.upeu.usuario.dto.ResumenSolicitudes;
import pe.edu.upeu.usuario.dto.RevisionRequest;
import pe.edu.upeu.usuario.dto.SolicitudDocenteResponse;
import pe.edu.upeu.usuario.service.SolicitudDocenteService;

/** Bandeja de verificación de docentes (solo rol ADMIN, exigido aquí y en el Gateway). */
@RestController
@RequestMapping("/api/v1/usuarios/admin/solicitudes")
public class AdminSolicitudController {

    private final SolicitudDocenteService solicitudes;

    public AdminSolicitudController(SolicitudDocenteService solicitudes) {
        this.solicitudes = solicitudes;
    }

    /** estado: ACTIVAS (por defecto: pendientes y observadas), TODAS o un estado concreto. */
    @GetMapping
    public List<SolicitudDocenteResponse> listar(@RequestParam(defaultValue = "ACTIVAS") String estado) {
        return solicitudes.listar(estado);
    }

    @GetMapping("/resumen")
    public ResumenSolicitudes resumen() {
        return solicitudes.resumen();
    }

    @GetMapping("/{id}")
    public SolicitudDocenteResponse obtener(@PathVariable Long id) {
        return solicitudes.obtener(id);
    }

    @GetMapping(path = "/{id}/documento", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> documento(@PathVariable Long id) {
        return DocumentoRespuesta.pdf(solicitudes.documento(id));
    }

    @PostMapping("/{id}/aprobar")
    public SolicitudDocenteResponse aprobar(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody(required = false) RevisionRequest revision) {
        return solicitudes.aprobar(id, revision == null ? null : revision.comentario(), revisor(jwt));
    }

    @PostMapping("/{id}/observar")
    public SolicitudDocenteResponse observar(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody RevisionRequest revision) {
        return solicitudes.observar(id, revision.comentario(), revisor(jwt));
    }

    @PostMapping("/{id}/rechazar")
    public SolicitudDocenteResponse rechazar(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody RevisionRequest revision) {
        return solicitudes.rechazar(id, revision.comentario(), revisor(jwt));
    }

    private static String revisor(Jwt jwt) {
        String usuario = jwt.getClaimAsString("preferred_username");
        return usuario == null ? jwt.getSubject() : usuario;
    }
}
