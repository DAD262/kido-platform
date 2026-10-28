package pe.edu.upeu.inscripcion.dto;
import java.time.LocalDateTime;
public record CertificadoDocumento(String codigo,String alumno,String curso,LocalDateTime fechaEmision) {}
