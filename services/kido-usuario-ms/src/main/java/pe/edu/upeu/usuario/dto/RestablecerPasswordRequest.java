package pe.edu.upeu.usuario.dto;
import jakarta.validation.constraints.*;
public record RestablecerPasswordRequest(@NotBlank @Size(min=8,max=72) String password,
    @NotNull Boolean temporal) {
    @Override public String toString() { return "RestablecerPasswordRequest[oculta]"; }
}
