package pe.edu.upeu.curso.dto;
import jakarta.validation.constraints.*; import java.math.BigDecimal;
public record ConfiguracionCursoRequest(@NotNull @Min(0) @Max(100) Integer progresoMinimo,@NotNull @Min(0) @Max(100) Integer asistenciaMinima,boolean certificadoHabilitado,@NotNull @DecimalMin("0.00") BigDecimal certificadoCosto){}
