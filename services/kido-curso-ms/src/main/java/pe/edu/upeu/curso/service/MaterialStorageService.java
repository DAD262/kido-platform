package pe.edu.upeu.curso.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import pe.edu.upeu.curso.dto.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

@Service @RequiredArgsConstructor
public class MaterialStorageService {
    private final ContenidoCursoService contenidoService;
    @Value("${kido.storage.materiales-dir:./uploads}") private String materialesDir;
    @Value("${kido.storage.max-bytes:52428800}") private long maxBytes;
    private static final Set<String> PERMITIDAS=Set.of("pdf","doc","docx","ppt","pptx","xls","xlsx","txt","zip","png","jpg","jpeg","webp","mp3","mp4","webm");
    public MaterialResponse guardar(Long leccionId, MultipartFile archivo, String nombre, String descripcion, boolean descargable) {
        if(archivo==null || archivo.isEmpty()) throw new IllegalArgumentException("Debe seleccionar un archivo");
        if(archivo.getSize()>maxBytes) throw new IllegalArgumentException("El archivo supera el tamaño permitido");
        String original=Optional.ofNullable(archivo.getOriginalFilename()).orElse("archivo");
        String ext=extension(original); if(!PERMITIDAS.contains(ext)) throw new IllegalArgumentException("Tipo de archivo no permitido: "+ext);
        String seguro=UUID.randomUUID()+"."+ext;
        try { Path dir=Paths.get(materialesDir).toAbsolutePath().normalize(); Files.createDirectories(dir); Path target=dir.resolve(seguro).normalize(); if(!target.startsWith(dir)) throw new IllegalArgumentException("Nombre de archivo inválido"); try(var in=archivo.getInputStream()){Files.copy(in,target,StandardCopyOption.REPLACE_EXISTING);} }
        catch(IOException e){throw new IllegalStateException("No se pudo almacenar el material",e);}
        String tipo=tipo(ext); String titulo=(nombre==null||nombre.isBlank())?original:nombre;
        return contenidoService.crearMaterial(leccionId,new MaterialRequest(titulo,tipo,"/api/v1/materiales/archivos/"+seguro,descripcion,descargable));
    }
    public Resource cargar(String nombre){
        if(nombre==null || nombre.contains("..") || nombre.contains("/") || nombre.contains("\\")) throw new IllegalArgumentException("Nombre de archivo inválido");
        try { Path p=Paths.get(materialesDir).toAbsolutePath().normalize().resolve(nombre).normalize(); Resource r=new UrlResource(p.toUri()); if(!r.exists()||!r.isReadable()) throw new IllegalArgumentException("Archivo no encontrado"); return r; }
        catch(Exception e){ if(e instanceof IllegalArgumentException ia) throw ia; throw new IllegalStateException("No se pudo leer el archivo",e); }
    }
    private String extension(String n){int i=n.lastIndexOf('.');return i<0?"":n.substring(i+1).toLowerCase(Locale.ROOT);}
    private String tipo(String e){return switch(e){case "pdf"->"PDF";case "png","jpg","jpeg","webp"->"IMAGEN";case "mp3"->"AUDIO";case "mp4","webm"->"VIDEO";case "doc","docx","ppt","pptx","xls","xlsx","txt","zip"->"DOCUMENTO";default->"OTRO";};}
}
