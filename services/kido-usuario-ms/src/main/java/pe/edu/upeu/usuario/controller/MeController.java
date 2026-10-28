package pe.edu.upeu.usuario.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import pe.edu.upeu.usuario.dto.DatosDocenteRequest;
import pe.edu.upeu.usuario.dto.PerfilResponse;
import pe.edu.upeu.usuario.dto.SolicitudDocenteResponse;
import pe.edu.upeu.usuario.service.PerfilService;
import pe.edu.upeu.usuario.service.SolicitudDocenteService;

/** Lo que la persona autenticada puede ver y hacer sobre su propia cuenta. */
@RestController
@RequestMapping("/api/v1/usuarios/me")
public class MeController {

    private final PerfilService perfiles;
    private final SolicitudDocenteService solicitudes;

    public MeController(PerfilService perfiles, SolicitudDocenteService solicitudes) {
        this.perfiles = perfiles;
        this.solicitudes = solicitudes;
    }

    @GetMapping
    public PerfilResponse me(@AuthenticationPrincipal Jwt jwt) {
        return perfiles.me(jwt);
    }

    @PostMapping(path = "/solicitud-docente", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public SolicitudDocenteResponse postular(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestPart("datos") DatosDocenteRequest datos,
            @RequestPart(value = "documento", required = false) MultipartFile documento) {
        return solicitudes.enviarPropia(jwt, datos, documento);
    }

    @GetMapping(path = "/solicitud-docente/documento", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> miDocumento(@AuthenticationPrincipal Jwt jwt) {
        return DocumentoRespuesta.pdf(solicitudes.documentoPropio(jwt));
    }
}
