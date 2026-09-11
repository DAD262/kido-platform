package pe.edu.upeu.inscripcion.dto;

import jakarta.validation.constraints.*;
import java.util.List;

public record CompraInscripcionRequest(
        @NotNull @Positive Long estudianteId,
        @NotNull @Positive Long cursoId,
        @NotNull List<@Positive Long> leccionIds
) {}
