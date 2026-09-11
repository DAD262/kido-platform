package pe.edu.upeu.curso.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record CursoRequest(
        @NotBlank @Size(max = 150) String titulo,
        @NotBlank @Size(max = 1000) String descripcion,
        @NotBlank String tipo,
        @NotNull @DecimalMin("0.00") BigDecimal precio,
        @NotNull @Positive Long docenteId,
        @NotNull @Positive Long categoriaId,
        String estado
) {}
