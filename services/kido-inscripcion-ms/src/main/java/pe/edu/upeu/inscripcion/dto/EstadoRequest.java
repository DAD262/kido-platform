package pe.edu.upeu.inscripcion.dto;

import jakarta.validation.constraints.NotBlank;
public record EstadoRequest(@NotBlank String estado) {}
