package pe.edu.upeu.inscripcion.dto;

import java.time.LocalDateTime;
import java.util.List;

public record InscripcionResponse(
    Long id, Long estudianteId, Long cursoId, String tipoAcceso, String estado,
    LocalDateTime fechaInscripcion, int porcentajeProgreso, List<ProgresoResponse> progresos
) {}
