package pe.edu.upeu.usuario.dto;

import jakarta.validation.constraints.Size;

public record RevisionRequest(@Size(max = 600, message = "Máximo 600 caracteres.") String comentario) {
}
