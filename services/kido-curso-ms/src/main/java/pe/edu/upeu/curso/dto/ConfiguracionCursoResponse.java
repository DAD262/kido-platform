package pe.edu.upeu.curso.dto;
import java.math.BigDecimal;
public record ConfiguracionCursoResponse(Long cursoId,Integer progresoMinimo,Integer asistenciaMinima,boolean certificadoHabilitado,BigDecimal certificadoCosto){}
