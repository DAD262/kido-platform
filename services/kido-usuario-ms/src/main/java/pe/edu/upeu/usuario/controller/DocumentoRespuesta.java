package pe.edu.upeu.usuario.controller;

import java.nio.charset.StandardCharsets;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import pe.edu.upeu.usuario.entity.DocumentoSolicitud;

final class DocumentoRespuesta {
    private DocumentoRespuesta() {
    }

    /** PDF en línea (se abre en el visor del navegador) y sin caché compartida. */
    static ResponseEntity<byte[]> pdf(DocumentoSolicitud doc) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename(doc.getNombre(), StandardCharsets.UTF_8).build().toString())
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .header("X-Content-Type-Options", "nosniff")
                .body(doc.getContenido());
    }
}
