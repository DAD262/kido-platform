package pe.edu.upeu.inscripcion.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record PuntuacionRequest(@NotNull @Min(1) @Max(5) Integer puntuacion) {}
