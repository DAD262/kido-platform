package pe.edu.upeu.inscripcion.dto;

import java.time.LocalDateTime;

public record ProgresoResponse(Long id, Long leccionId, boolean completada, LocalDateTime fechaCompletada) {}
