package pe.edu.upeu.curso.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import pe.edu.upeu.curso.dto.CursoResponse;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
@RequiredArgsConstructor
public class CursoPortadaStorageService {
    private static final Set<String> EXTENSIONES = Set.of("png", "jpg", "jpeg", "webp");
    private final CursoService cursoService;

    @Value("${kido.storage.portadas-dir:./uploads/portadas}") private String portadasDir;
    @Value("${kido.storage.portadas-max-bytes:26214400}") private long maxBytes;

    public CursoResponse guardar(Long cursoId, MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) throw new IllegalArgumentException("Selecciona una imagen de portada");
        if (archivo.getSize() > maxBytes) throw new IllegalArgumentException("La portada supera el límite de 25 MB");
        String original = Optional.ofNullable(archivo.getOriginalFilename()).orElse("portada");
        String extension = extension(original);
        if (!EXTENSIONES.contains(extension)) throw new IllegalArgumentException("Usa una imagen PNG, JPG, JPEG o WebP");
        try {
            if (!imagenValida(archivo, extension)) throw new IllegalArgumentException("El archivo no contiene una imagen válida");
            Path dir = directorio();
            Files.createDirectories(dir);
            CursoResponse anterior = cursoService.obtener(cursoId);
            if (!"PUBLICADO".equals(anterior.estado()))
                throw new IllegalStateException("El administrador debe aprobar y publicar el curso antes de guardar su portada");
            String nombre = "curso-" + cursoId + "-" + UUID.randomUUID() + "." + extension;
            Path destino = dir.resolve(nombre).normalize();
            if (!destino.startsWith(dir)) throw new IllegalArgumentException("Nombre de archivo inválido");
            try (var entrada = archivo.getInputStream()) {
                Files.copy(entrada, destino, StandardCopyOption.REPLACE_EXISTING);
            }
            CursoResponse actualizado = cursoService.actualizarPortada(cursoId, "/api/v1/cursos/portadas/" + nombre);
            eliminarArchivo(anterior.portadaUrl());
            return actualizado;
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo almacenar la portada", e);
        }
    }

    public CursoResponse eliminar(Long cursoId) {
        CursoResponse anterior = cursoService.obtener(cursoId);
        if (!"PUBLICADO".equals(anterior.estado()))
            throw new IllegalStateException("Solo puedes modificar la portada de un curso publicado");
        CursoResponse actualizado = cursoService.actualizarPortada(cursoId, null);
        eliminarArchivo(anterior.portadaUrl());
        return actualizado;
    }

    public Resource cargar(String nombre) {
        if (nombre == null || nombre.contains("..") || nombre.contains("/") || nombre.contains("\\"))
            throw new IllegalArgumentException("Nombre de portada inválido");
        try {
            Path archivo = directorio().resolve(nombre).normalize();
            if (!archivo.startsWith(directorio())) throw new IllegalArgumentException("Nombre de portada inválido");
            Resource recurso = new UrlResource(archivo.toUri());
            if (!recurso.exists() || !recurso.isReadable()) throw new IllegalArgumentException("Portada no encontrada");
            return recurso;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo leer la portada", e);
        }
    }

    private void eliminarArchivo(String url) {
        if (url == null || !url.startsWith("/api/v1/cursos/portadas/")) return;
        String nombre = url.substring(url.lastIndexOf('/') + 1);
        try { Files.deleteIfExists(directorio().resolve(nombre).normalize()); }
        catch (IOException ignored) { /* La referencia ya fue retirada; la limpieza puede reintentarse. */ }
    }

    private Path directorio() { return Paths.get(portadasDir).toAbsolutePath().normalize(); }
    private boolean imagenValida(MultipartFile archivo, String extension) throws IOException {
        if (!"webp".equals(extension)) {
            try (var entrada = archivo.getInputStream()) { return ImageIO.read(entrada) != null; }
        }
        try (var entrada = archivo.getInputStream()) {
            byte[] cabecera = entrada.readNBytes(12);
            return cabecera.length == 12
                    && "RIFF".equals(new String(cabecera, 0, 4, StandardCharsets.US_ASCII))
                    && "WEBP".equals(new String(cabecera, 8, 4, StandardCharsets.US_ASCII));
        }
    }
    private String extension(String nombre) {
        int punto = nombre.lastIndexOf('.');
        return punto < 0 ? "" : nombre.substring(punto + 1).toLowerCase(Locale.ROOT);
    }
}
