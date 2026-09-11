package pe.edu.upeu.inscripcion.dto;

import jakarta.validation.constraints.*;
import java.util.List;

public record InscripcionRequest(
    @NotNull @Positive Long estudianteId,
    @NotNull @Positive Long cursoId,
    @NotBlank String tipoAcceso,
    @NotEmpty List<@Positive Long> leccionIds
) {}
